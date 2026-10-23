package com.dtthuan3.ecommerce.authservice.keycloak;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
            List<String> requiredActions) {}

    /** Realm role kem so member, tra ve cho Admin Roles API. */
    public record RealmRole(String name, String description, long memberCount) {}

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
                strList(user.get("requiredActions")));
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
                    .uri(adminBase() + "/roles/" + roleName + "/users?max=1000")
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
                    .uri(adminBase() + "/roles/" + roleName + "/users?max=1000")
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
                    .uri(adminBase() + "/roles/" + roleName)
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
}
