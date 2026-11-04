package com.dtthuan3.ecommerce.notificationservice.provider;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import java.time.Instant;
import java.util.Map;

/**
 * Resolve userId -> email qua Keycloak Admin API (cung pattern KeycloakAdminClient
 * ben auth-service: client_credentials token + GET /admin/realms/{realm}/users/{id}).
 * De trong admin client-id/secret = tat resolve (mail se FAILED_RETRYABLE kem ly do
 * ro rang, khong gui mù). Cache token nhu auth-service.
 */
@Slf4j
@Component
public class KeycloakRecipientResolver {

    private final RestClient restClient;
    private final String serverUrl;
    private final String realm;
    private final String adminClientId;
    private final String adminClientSecret;
    private final String adminRealm;

    private volatile String cachedToken;
    private volatile Instant tokenExpiresAt = Instant.EPOCH;

    public KeycloakRecipientResolver(
            RestClient.Builder restClientBuilder,
            @Value("${auth.keycloak.server-url:http://162.4.177.91:7080}") String serverUrl,
            @Value("${auth.keycloak.realm:ecommerce}") String realm,
            @Value("${auth.keycloak.admin.client-id:}") String adminClientId,
            @Value("${auth.keycloak.admin.client-secret:}") String adminClientSecret,
            @Value("${auth.keycloak.admin.realm:master}") String adminRealm) {
        this.restClient = restClientBuilder.build();
        this.serverUrl = serverUrl;
        this.realm = realm;
        this.adminClientId = adminClientId;
        this.adminClientSecret = adminClientSecret;
        this.adminRealm = adminRealm;
    }

    public boolean isEnabled() {
        return adminClientId != null && !adminClientId.isBlank()
                && adminClientSecret != null && !adminClientSecret.isBlank();
    }

    /**
     * Tra email cua user theo userId, hoac null khi khong resolve duoc
     * (chua cau hinh admin client, Keycloak down, user khong co email).
     */
    public String resolveEmail(String userId) {
        if (!isEnabled() || userId == null || userId.isBlank()) return null;
        try {
            Map<String, Object> user = restClient
                    .get()
                    .uri(serverUrl + "/admin/realms/" + realm + "/users/" + userId)
                    .headers(h -> h.setBearerAuth(adminToken()))
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});
            if (user == null) return null;
            Object email = user.get("email");
            return email == null ? null : String.valueOf(email);
        } catch (RestClientException e) {
            log.warn("[MAIL] Resolve email userId={} that bai: {}", userId, e.getMessage());
            return null;
        }
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
}
