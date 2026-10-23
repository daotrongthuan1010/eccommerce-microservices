package com.dtthuan3.ecommerce.authservice.keycloak;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

/**
 * Client goi Keycloak Admin REST API bang service account (client_credentials).
 *
 * <p>Dung cho dang ky, quen mat khau, doi mat khau, verify email - vi realm DEV co
 * {@code registrationAllowed=false} nen khong the self-register bang API cong khai.
 * Neu {@code auth.keycloak.admin.client-secret} de trong thi client o che do disabled
 * va moi thao tac nem {@link KeycloakAdminDisabledException} (controller tra 501
 * kem huong dan thu cong).
 */
@Service
public class KeycloakAdminClient {

    private static final Logger log = LoggerFactory.getLogger(KeycloakAdminClient.class);

    private final RestClient restClient = RestClient.create();

    @Value("${auth.keycloak.server-url:http://162.4.177.91:7080}")
    private String serverUrl;

    @Value("${auth.keycloak.realm:ecommerce}")
    private String realm;

    @Value("${auth.keycloak.admin.client-id:}")
    private String adminClientId;

    @Value("${auth.keycloak.admin.client-secret:}")
    private String adminClientSecret;

    @Value("${auth.keycloak.admin.realm:master}")
    private String adminRealm;

    @Value("${auth.keycloak.client-id:ecommerce-app}")
    private String publicClientId;

    private volatile String cachedToken;
    private volatile Instant tokenExpiresAt = Instant.EPOCH;

    public boolean isEnabled() {
        return adminClientId != null
                && !adminClientId.isBlank()
                && adminClientSecret != null
                && !adminClientSecret.isBlank();
    }

    public void requireEnabled() {
        if (!isEnabled()) {
            throw new KeycloakAdminDisabledException();
        }
    }

    private String adminBase() {
        return serverUrl + "/admin/realms/" + realm;
    }

    private synchronized String adminToken() {
        if (cachedToken != null && Instant.now().isBefore(tokenExpiresAt.minusSeconds(30))) {
            return cachedToken;
        }
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", adminClientId);
        form.add("client_secret", adminClientSecret);
        Map<?, ?> token = restClient
                .post()
                .uri(serverUrl + "/realms/" + adminRealm + "/protocol/openid-connect/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(Map.class);
        if (token == null || token.get("access_token") == null) {
            throw new IllegalStateException("Keycloak admin token response is empty");
        }
        cachedToken = String.valueOf(token.get("access_token"));
        long expiresIn = token.get("expires_in") instanceof Number n ? n.longValue() : 60L;
        tokenExpiresAt = Instant.now().plusSeconds(expiresIn);
        return cachedToken;
    }

    private HttpHeaders bearerHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken());
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    /**
     * Tao user Keycloak, tra ve userId (sub). Nem {@link UserAlreadyExistsException}
     * khi username hoac email da ton tai (HTTP 409 tu Keycloak).
     */
    public String createUser(String username, String email, String firstName, String lastName, String password) {
        requireEnabled();
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("username", username);
        body.put("email", email);
        body.put("firstName", firstName);
        body.put("lastName", lastName);
        body.put("enabled", true);
        body.put("emailVerified", false);
        body.put("credentials", List.of(Map.of("type", "password", "value", password, "temporary", false)));
        try {
            var response = restClient
                    .post()
                    .uri(adminBase() + "/users")
                    .headers(h -> h.addAll(bearerHeaders()))
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
            String location = response.getHeaders().getFirst(HttpHeaders.LOCATION);
            if (location == null || location.isBlank()) {
                // Keycloak luon tra Location .../users/{id}; fallback: tim theo username.
                String fallbackId = findUserId(username);
                if (fallbackId == null || fallbackId.isBlank()) {
                    throw new IllegalStateException(
                            "Keycloak created user but returned no Location header and lookup found nothing: " + username);
                }
                return fallbackId;
            }
            return location.substring(location.lastIndexOf('/') + 1);
        } catch (org.springframework.web.client.HttpClientErrorException.Conflict ex) {
            throw new UserAlreadyExistsException(username);
        }
    }

    /**
     * Gan realm role cho user (strict: nem {@link RealmRoleNotFoundException} khi role la,
     * {@link UserNotFoundException} khi userId la). Dung cho Admin API de response
     * khong bao success gia khi Keycloak that bai.
     */
    public void assignRealmRole(String userId, String roleName) {
        requireEnabled();
        Map<String, Object> role = getRealmRole(roleName);
        withRetry(() -> {
            try {
                restClient
                        .post()
                        .uri(adminBase() + "/users/" + userId + "/role-mappings/realm")
                        .headers(h -> h.addAll(bearerHeaders()))
                        .body(List.of(role))
                        .retrieve()
                        .toBodilessEntity();
            } catch (HttpClientErrorException.NotFound ex) {
                throw new UserNotFoundException(userId);
            }
            return null;
        });
    }

    /**
     * Bien the best-effort cua {@link #assignRealmRole} chi dung cho register cong khai:
     * role USER mac dinh thieu khong duoc chan dang ky, chi log warn.
     */
    public void assignRealmRoleBestEffort(String userId, String roleName) {
        try {
            assignRealmRole(userId, roleName);
        } catch (Exception ex) {
            log.warn("Assign realm role {} to user {} failed (best-effort register path): {}",
                    roleName, userId, ex.getMessage());
        }
    }

    /** Tim userId theo username (exact match), tra null neu khong thay. */
    public String findUserId(String username) {
        requireEnabled();
        List<Map<String, Object>> users = restClient
                .get()
                .uri(adminBase() + "/users?username={u}&exact=true", username)
                .headers(h -> h.addAll(bearerHeaders()))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
        if (users == null || users.isEmpty()) {
            return null;
        }
        Object id = users.get(0).get("id");
        return id == null ? null : String.valueOf(id);
    }

    /** Gui email thuc hien required action (UPDATE_PASSWORD / VERIFY_EMAIL). */
    public void sendRequiredActionEmail(String userId, String action) {
        requireEnabled();
        restClient
                .put()
                .uri(adminBase() + "/users/{id}/execute-actions-email?client_id={c}", userId, publicClientId)
                .headers(h -> h.addAll(bearerHeaders()))
                .body(List.of(action))
                .retrieve()
                .toBodilessEntity();
    }

    /** Dat lai mat khau truc tiep (khong gui email). */
    public void resetPassword(String userId, String newPassword) {
        requireEnabled();
        restClient
                .put()
                .uri(adminBase() + "/users/{id}/reset-password", userId)
                .headers(h -> h.addAll(bearerHeaders()))
                .body(Map.of("type", "password", "value", newPassword, "temporary", false))
                .retrieve()
                .toBodilessEntity();
    }

    public static class UserAlreadyExistsException extends RuntimeException {
        public UserAlreadyExistsException(String username) {
            super("User already exists: " + username);
        }
    }

    public static class UserNotFoundException extends RuntimeException {
        public UserNotFoundException(String userId) {
            super("User not found: " + userId);
        }
    }

    public static class RealmRoleNotFoundException extends RuntimeException {
        public RealmRoleNotFoundException(String roleName) {
            super("Realm role not found: " + roleName);
        }
    }

    /** Tom tat user Keycloak kem realm roles, tra ve cho Admin API. */
    public record AdminUser(
            String id,
            String username,
            String email,
            String firstName,
            String lastName,
            boolean enabled,
            boolean emailVerified,
            List<String> roles,
            List<String> requiredActions,
            String picture) {}

    /** Realm role kem so member, tra ve cho Admin Roles API. */
    public record RealmRole(String name, String description, long memberCount) {}

    public static class EmailAlreadyExistsException extends RuntimeException {
        public EmailAlreadyExistsException(String email) {
            super("Email already exists: " + email);
        }
    }

    public static class RoleAlreadyExistsException extends RuntimeException {
        public RoleAlreadyExistsException(String roleName) {
            super("Realm role already exists: " + roleName);
        }
    }

    /**
     * Goi Keycloak, tu refresh service-account token 1 lan khi gap 401
     * roi thu lai; het han token giua chung khong lam hong request.
     */
    private <T> T withRetry(Supplier<T> call) {
        try {
            return call.get();
        } catch (HttpClientErrorException.Unauthorized ex) {
            log.info("Keycloak admin token expired, refresh once and retry");
            cachedToken = null;
            return call.get();
        }
    }

    private static String encodeRole(String roleName) {
        return URLEncoder.encode(roleName, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private String buildUsersUri(String search, Integer first, Integer max) {
        StringBuilder sb = new StringBuilder(adminBase()).append("/users?briefRepresentation=false");
        if (search != null && !search.isBlank()) {
            sb.append("&search=").append(URLEncoder.encode(search.trim(), StandardCharsets.UTF_8));
        }
        if (first != null) {
            sb.append("&first=").append(Math.max(first, 0));
        }
        if (max != null) {
            sb.append("&max=").append(Math.min(Math.max(max, 1), 50));
        }
        return sb.toString();
    }

    private static String str(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    @SuppressWarnings("unchecked")
    private static List<String> strList(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        List<String> result = new ArrayList<>(list.size());
        for (Object item : list) {
            if (item != null) {
                result.add(String.valueOf(item));
            }
        }
        return result;
    }

    private AdminUser toAdminUser(Map<String, Object> user, List<String> roles) {
        return new AdminUser(
                str(user.get("id")),
                str(user.get("username")),
                str(user.get("email")),
                str(user.get("firstName")),
                str(user.get("lastName")),
                Boolean.TRUE.equals(user.get("enabled")),
                Boolean.TRUE.equals(user.get("emailVerified")),
                roles == null ? List.of() : List.copyOf(roles),
                strList(user.get("requiredActions")),
                firstAttribute(user.get("attributes"), "picture"));
    }

    /** Lay gia tri dau tien cua attribute Keycloak (VD attributes.picture=[file-key]). */
    @SuppressWarnings("unchecked")
    private static String firstAttribute(Object attributes, String key) {
        if (!(attributes instanceof Map<?, ?> map)) {
            return null;
        }
        Object value = map.get(key);
        if (value instanceof List<?> list) {
            return list.isEmpty() || list.get(0) == null
                    ? null
                    : String.valueOf(list.get(0));
        }
        return value == null ? null : String.valueOf(value);
    }

    /** Realm roles cua user (chi lay name). Khong retry rieng - chay trong withRetry ngoai. */
    private List<String> fetchRoleNames(String userId) {
        List<Map<String, Object>> mappings = restClient
                .get()
                .uri(adminBase() + "/users/" + userId + "/role-mappings/realm")
                .headers(h -> h.addAll(bearerHeaders()))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
        if (mappings == null) {
            return List.of();
        }
        List<String> names = new ArrayList<>(mappings.size());
        for (Map<String, Object> mapping : mappings) {
            Object name = mapping.get("name");
            if (name != null) {
                names.add(String.valueOf(name));
            }
        }
        return names;
    }

    /** Liet ke user Keycloak (search theo username/email), kem roles tung user. */
    public List<AdminUser> listUsers(String search, int first, int max) {
        requireEnabled();
        return withRetry(() -> {
            List<Map<String, Object>> users = restClient
                    .get()
                    .uri(buildUsersUri(search, first, max))
                    .headers(h -> h.addAll(bearerHeaders()))
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            if (users == null || users.isEmpty()) {
                return List.of();
            }
            List<AdminUser> result = new ArrayList<>(users.size());
            for (Map<String, Object> user : users) {
                String id = str(user.get("id"));
                result.add(toAdminUser(user, id == null ? List.of() : fetchRoleNames(id)));
            }
            return result;
        });
    }

    /**
     * Dem user Keycloak (phuc vu page envelope). Tra -1 khi endpoint
     * /users/count khong kha dung de controller fallback.
     */
    public long countUsers(String search) {
        requireEnabled();
        StringBuilder sb = new StringBuilder(adminBase()).append("/users/count");
        if (search != null && !search.isBlank()) {
            sb.append("?search=").append(URLEncoder.encode(search.trim(), StandardCharsets.UTF_8));
        }
        String uri = sb.toString();
        try {
            return withRetry(() -> {
                Number count = restClient
                        .get()
                        .uri(uri)
                        .headers(h -> h.addAll(bearerHeaders()))
                        .retrieve()
                        .body(Number.class);
                return count == null ? 0L : count.longValue();
            });
        } catch (Exception ex) {
            log.warn("Count Keycloak users failed, fallback to page size: {}", ex.getMessage());
            return -1L;
        }
    }

    /** Chi tiet user kem roles. Nem {@link UserNotFoundException} khi id la. */
    public AdminUser getUserWithRoles(String userId) {
        requireEnabled();
        return withRetry(() -> {
            try {
                Map<String, Object> user = restClient
                        .get()
                        .uri(adminBase() + "/users/" + userId)
                        .headers(h -> h.addAll(bearerHeaders()))
                        .retrieve()
                        .body(new ParameterizedTypeReference<Map<String, Object>>() {});
                if (user == null || user.get("id") == null) {
                    throw new UserNotFoundException(userId);
                }
                return toAdminUser(user, fetchRoleNames(userId));
            } catch (HttpClientErrorException.NotFound ex) {
                throw new UserNotFoundException(userId);
            }
        });
    }

    /** Toan bo realm roles kem member count (chi vai role nen resolve truc tiep). */
    public List<RealmRole> listRealmRoles() {
        requireEnabled();
        return withRetry(() -> {
            List<Map<String, Object>> roles = restClient
                    .get()
                    .uri(adminBase() + "/roles")
                    .headers(h -> h.addAll(bearerHeaders()))
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            if (roles == null || roles.isEmpty()) {
                return List.of();
            }
            List<RealmRole> result = new ArrayList<>(roles.size());
            for (Map<String, Object> role : roles) {
                String name = str(role.get("name"));
                if (name == null) {
                    continue;
                }
                result.add(new RealmRole(name, str(role.get("description")), countRoleMembers(name)));
            }
            return result;
        });
    }

    private long countRoleMembers(String roleName) {
        try {
            List<?> members = restClient
                    .get()
                    .uri(adminBase() + "/roles/" + encodeRole(roleName) + "/users?max=1000")
                    .headers(h -> h.addAll(bearerHeaders()))
                    .retrieve()
                    .body(List.class);
            return members == null ? 0L : members.size();
        } catch (Exception ex) {
            log.warn("Count members of role {} failed: {}", roleName, ex.getMessage());
            return 0L;
        }
    }

    /** Members cua 1 realm role (khong kem roles tung user de tranh N+1). */
    public List<AdminUser> listUsersInRole(String roleName) {
        requireEnabled();
        getRealmRole(roleName);
        return withRetry(() -> {
            List<Map<String, Object>> users = restClient
                    .get()
                    .uri(adminBase() + "/roles/" + encodeRole(roleName) + "/users?max=1000")
                    .headers(h -> h.addAll(bearerHeaders()))
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            if (users == null || users.isEmpty()) {
                return List.of();
            }
            List<AdminUser> result = new ArrayList<>(users.size());
            for (Map<String, Object> user : users) {
                result.add(toAdminUser(user, List.of(roleName)));
            }
            return result;
        });
    }

    /** Lay 1 realm role; nem {@link RealmRoleNotFoundException} khi ten la. */
    public Map<String, Object> getRealmRole(String roleName) {
        requireEnabled();
        try {
            return withRetry(() -> restClient
                    .get()
                    .uri(adminBase() + "/roles/" + encodeRole(roleName))
                    .headers(h -> h.addAll(bearerHeaders()))
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {}));
        } catch (HttpClientErrorException.NotFound ex) {
            throw new RealmRoleNotFoundException(roleName);
        }
    }

    /**
     * Thu hoi realm role (strict: 404 khi user/role khong ton tai).
     * Doi xung voi {@link #assignRealmRole} strict dung cho Admin API.
     */
    public void removeRealmRole(String userId, String roleName) {
        requireEnabled();
        Map<String, Object> role = getRealmRole(roleName);
        withRetry(() -> {
            try {
                restClient
                        .method(HttpMethod.DELETE)
                        .uri(adminBase() + "/users/" + userId + "/role-mappings/realm")
                        .headers(h -> h.addAll(bearerHeaders()))
                        .body(List.of(role))
                        .retrieve()
                        .toBodilessEntity();
            } catch (HttpClientErrorException.NotFound ex) {
                throw new UserNotFoundException(userId);
            }
            return null;
        });
    }

    /** Bat/tat user. Nem {@link UserNotFoundException} khi id la. */
    public void setEnabled(String userId, boolean enabled) {
        requireEnabled();
        withRetry(() -> {
            try {
                restClient
                        .put()
                        .uri(adminBase() + "/users/" + userId)
                        .headers(h -> h.addAll(bearerHeaders()))
                        .body(Map.of("enabled", enabled))
                        .retrieve()
                        .toBodilessEntity();
            } catch (HttpClientErrorException.NotFound ex) {
                throw new UserNotFoundException(userId);
            }
            return null;
        });
    }

    /** Dat lai mat khau voi co temporary (temporary=true bat buoc doi o lan login sau). */
    public void resetPassword(String userId, String newPassword, boolean temporary) {
        requireEnabled();
        restClient
                .put()
                .uri(adminBase() + "/users/" + userId + "/reset-password")
                .headers(h -> h.addAll(bearerHeaders()))
                .body(Map.of("type", "password", "value", newPassword, "temporary", temporary))
                .retrieve()
                .toBodilessEntity();
    }

    /** Them required action (VD UPDATE_PASSWORD) giu nguyen danh sach hien co. */
    public void addRequiredAction(String userId, String action) {
        requireEnabled();
        AdminUser user = getUserWithRoles(userId);
        if (user.requiredActions().contains(action)) {
            return;
        }
        List<String> merged = new ArrayList<>(user.requiredActions());
        merged.add(action);
        restClient
                .put()
                .uri(adminBase() + "/users/" + userId)
                .headers(h -> h.addAll(bearerHeaders()))
                .body(Map.of("requiredActions", merged))
                .retrieve()
                .toBodilessEntity();
    }

    /**
     * Doc full user representation tu Keycloak (GET). Dung lam nen cho PUT
     * merge - Keycloak PUT users/{id} thay the representation nen gui body
     * thua (sparse) co the xoa nham field tuy version.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> getRawUser(String userId) {
        try {
            Map<String, Object> user = restClient
                    .get()
                    .uri(adminBase() + "/users/" + userId)
                    .headers(h -> h.addAll(bearerHeaders()))
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});
            if (user == null || user.get("id") == null) {
                throw new UserNotFoundException(userId);
            }
            return new LinkedHashMap<>(user);
        } catch (HttpClientErrorException.NotFound ex) {
            throw new UserNotFoundException(userId);
        }
    }

    /**
     * Tu sua profile cua chinh user (firstName/lastName/email). GET full
     * representation truoc roi PUT merge de khong xoa nham field/attribute
     * (Keycloak PUT thay the). Email trung -> 409
     * {@link EmailAlreadyExistsException}. Doi email -> reset
     * {@code emailVerified=false} (tranh flag stale). Khong co gi doi ->
     * bo qua PUT, chi GET tra ve.
     */
    public AdminUser updateUserProfile(String userId, String firstName, String lastName, String email) {
        requireEnabled();
        withRetry(() -> {
            try {
                Map<String, Object> raw = getRawUser(userId);
                boolean changed = false;
                if (firstName != null && !firstName.isBlank()) {
                    String next = firstName.trim();
                    if (!next.equals(str(raw.get("firstName")))) {
                        raw.put("firstName", next);
                        changed = true;
                    }
                }
                if (lastName != null && !lastName.isBlank()) {
                    String next = lastName.trim();
                    if (!next.equals(str(raw.get("lastName")))) {
                        raw.put("lastName", next);
                        changed = true;
                    }
                }
                if (email != null && !email.isBlank()) {
                    String next = email.trim();
                    if (!next.equalsIgnoreCase(str(raw.get("email")))) {
                        raw.put("email", next);
                        // Email doi -> reset verified de tranh flag stale; user xac thuc lai qua /send-verify-email.
                        raw.put("emailVerified", false);
                        changed = true;
                    }
                }
                if (!changed) {
                    return null;
                }
                restClient
                        .put()
                        .uri(adminBase() + "/users/" + userId)
                        .headers(h -> h.addAll(bearerHeaders()))
                        .body(raw)
                        .retrieve()
                        .toBodilessEntity();
            } catch (HttpClientErrorException.NotFound ex) {
                throw new UserNotFoundException(userId);
            } catch (HttpClientErrorException.Conflict ex) {
                throw new EmailAlreadyExistsException(email);
            } catch (HttpClientErrorException.BadRequest ex) {
                // Mot so version Keycloak tra 400 (thay vi 409) khi email da ton tai.
                String responseBody = ex.getResponseBodyAsString();
                if (responseBody != null && responseBody.toLowerCase().contains("email")) {
                    throw new EmailAlreadyExistsException(email);
                }
                throw ex;
            }
            return null;
        });
        return getUserWithRoles(userId);
    }

    /**
     * Luu file key anh dai dien vao attribute {@code picture} cua user.
     * Chi luu key (VD uploads/avatars/xxx.png), KHONG luu presigned URL -
     * client resolve URL tuoi tho ngan qua file-service {@code GET /files/url}
     * moi lan hien thi. Xoa avatar: truyen key null/blank.
     *
     * <p>GET full representation truoc roi PUT merge (giu nguyen cac
     * attribute khac) - Keycloak PUT thay the toan bo attributes map.
     */
    public AdminUser setUserPicture(String userId, String fileKey) {
        requireEnabled();
        String expect = (fileKey == null || fileKey.isBlank()) ? null : fileKey.trim();
        withRetry(() -> {
            try {
                Map<String, Object> raw = getRawUser(userId);
                // Chuan hoa toan bo attributes ve Map<String, List<String>> dung
                // UserRepresentation Keycloak: value la (VD String don) khien
                // Keycloak bo qua ca map attributes o PUT, lam mat picture.
                Map<String, List<String>> attrs = new LinkedHashMap<>();
                if (raw.get("attributes") instanceof Map<?, ?> existing) {
                    existing.forEach((k, v) -> {
                        List<String> vals = toStringList(v);
                        if (!vals.isEmpty()) {
                            attrs.put(String.valueOf(k), vals);
                        }
                    });
                }
                if (expect == null) {
                    attrs.remove("picture");
                } else {
                    attrs.put("picture", List.of(expect));
                }
                raw.put("attributes", attrs);
                restClient
                        .put()
                        .uri(adminBase() + "/users/" + userId)
                        .headers(h -> h.addAll(bearerHeaders()))
                        .body(raw)
                        .retrieve()
                        .toBodilessEntity();
                // Read-back xac minh persist (GET-merge-PUT tung lam mat attribute
                // ma van tra 200): mismatch = fail request de FE hien loi thay vi
                // toast success gia.
                try {
                    String persisted = firstAttribute(getRawUser(userId).get("attributes"), "picture");
                    if (!Objects.equals(expect, persisted)) {
                        throw new IllegalStateException("Keycloak did not persist picture for user "
                                + userId + " (expected=" + expect + ", read-back=" + persisted + ")");
                    }
                } catch (IllegalStateException ex) {
                    throw ex;
                } catch (Exception ex) {
                    log.warn("setUserPicture read-back verify failed for user {}: {}", userId, ex.getMessage());
                }
            } catch (HttpClientErrorException.NotFound ex) {
                throw new UserNotFoundException(userId);
            }
            return null;
        });
        return getUserWithRoles(userId);
    }

    /**
     * Chuan hoa 1 attribute value Keycloak ve List&lt;String&gt;: List giu nguyen
     * (stringify tung phan tu), String don boc thanh 1 phan tu, null/other rong.
     */
    private static List<String> toStringList(Object value) {
        if (value instanceof List<?> list) {
            List<String> result = new ArrayList<>(list.size());
            for (Object item : list) {
                if (item != null) {
                    result.add(String.valueOf(item));
                }
            }
            return result;
        }
        if (value instanceof String s) {
            return s.isEmpty() ? List.of() : List.of(s);
        }
        return value == null ? List.of() : List.of(String.valueOf(value));
    }

    /**
     * Tao realm role moi (ADMIN only o controller). Ten da ton tai -> 409
     * {@link RoleAlreadyExistsException}.
     */
    public void createRealmRole(String roleName, String description) {
        requireEnabled();
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("name", roleName);
        if (description != null && !description.isBlank()) {
            body.put("description", description.trim());
        }
        try {
            withRetry(() -> restClient
                    .post()
                    .uri(adminBase() + "/roles")
                    .headers(h -> h.addAll(bearerHeaders()))
                    .body(body)
                    .retrieve()
                    .toBodilessEntity());
        } catch (HttpClientErrorException.Conflict ex) {
            throw new RoleAlreadyExistsException(roleName);
        }
    }

    /** Xoa realm role. Nem {@link RealmRoleNotFoundException} khi ten la. */
    public void deleteRealmRole(String roleName) {
        requireEnabled();
        try {
            withRetry(() -> {
                restClient
                        .method(HttpMethod.DELETE)
                        .uri(adminBase() + "/roles/" + encodeRole(roleName))
                        .headers(h -> h.addAll(bearerHeaders()))
                        .retrieve()
                        .toBodilessEntity();
                return null;
            });
        } catch (HttpClientErrorException.NotFound ex) {
            throw new RealmRoleNotFoundException(roleName);
        }
    }
}
