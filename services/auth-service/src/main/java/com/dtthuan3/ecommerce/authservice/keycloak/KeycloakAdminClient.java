package com.dtthuan3.ecommerce.authservice.keycloak;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
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
                return findUserId(username);
            }
            return location.substring(location.lastIndexOf('/') + 1);
        } catch (org.springframework.web.client.HttpClientErrorException.Conflict ex) {
            throw new UserAlreadyExistsException(username);
        }
    }

    /** Gan realm role (mac dinh USER) cho user. Best-effort: log warn neu role khong ton tai. */
    public void assignRealmRole(String userId, String roleName) {
        requireEnabled();
        try {
            Map<?, ?> role = restClient
                    .get()
                    .uri(adminBase() + "/roles/" + roleName)
                    .headers(h -> h.addAll(bearerHeaders()))
                    .retrieve()
                    .body(Map.class);
            if (role == null) {
                log.warn("Keycloak realm role {} not found, skip assigning to user {}", roleName, userId);
                return;
            }
            restClient
                    .post()
                    .uri(adminBase() + "/users/" + userId + "/role-mappings/realm")
                    .headers(h -> h.addAll(bearerHeaders()))
                    .body(List.of(role))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception ex) {
            log.warn("Assign realm role {} to user {} failed: {}", roleName, userId, ex.getMessage());
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
}
