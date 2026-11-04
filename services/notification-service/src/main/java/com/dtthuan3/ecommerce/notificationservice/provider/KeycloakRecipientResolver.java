package com.dtthuan3.ecommerce.notificationservice.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Resolve userId -> (email, displayName) qua Keycloak Admin API (cung pattern
 * KeycloakAdminClient cua auth-service: client_credentials + GET /users/{id}).
 *
 * Cache userId -> Found/UserNotFound voi TTL: load test 1000 user lap lai 100 lan
 * se khong goi Keycloak 100k lan. Ket qua loi (Unavailable) khong cache de retry.
 */
@Slf4j
@Component
public class KeycloakRecipientResolver {

    private static final Duration CACHE_TTL = Duration.ofMinutes(10);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String serverUrl;
    private final String realm;
    private final String adminClientId;
    private final String adminClientSecret;
    private final String adminRealm;

    private volatile String cachedToken;
    private volatile Instant tokenExpiresAt = Instant.EPOCH;

    /** Cache ket qua resolve (chi Found/UserNotFound/NoEmail — ket qua tam thuat khong cache). */
    private record Cached(RecipientResolveResult result, Instant expiresAt) {}
    private final Map<String, Cached> cache = new ConcurrentHashMap<>();

    public KeycloakRecipientResolver(
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            @Value("${auth.keycloak.server-url:http://162.4.177.91:7080}") String serverUrl,
            @Value("${auth.keycloak.realm:ecommerce}") String realm,
            @Value("${auth.keycloak.admin.client-id:}") String adminClientId,
            @Value("${auth.keycloak.admin.client-secret:}") String adminClientSecret,
            @Value("${auth.keycloak.admin.realm:master}") String adminRealm) {
        this.restClient = restClientBuilder.build();
        this.objectMapper = objectMapper;
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
     * Resolve nguoi nhan theo userId. Ket qua typed:
     * Found / UserNotFound (permanent) / NoEmail (permanent) / Unavailable (retryable).
     */
    public RecipientResolveResult resolve(String userId) {
        if (userId == null || userId.isBlank()) {
            return new RecipientResolveResult.Unavailable("userId rong");
        }
        if (!isEnabled()) {
            return new RecipientResolveResult.Unavailable(
                    "Chua cau hinh Keycloak admin client (auth.keycloak.admin.client-id/secret)");
        }
        Cached hit = cache.get(userId);
        if (hit != null && Instant.now().isBefore(hit.expiresAt())) {
            return hit.result();
        }
        RecipientResolveResult result = doResolve(userId);
        if (result instanceof RecipientResolveResult.Unavailable) {
            return result; // loi tam thuat: khong cache de lan sau retry
        }
        cache.put(userId, new Cached(result, Instant.now().plus(CACHE_TTL)));
        return result;
    }

    private RecipientResolveResult doResolve(String userId) {
        try {
            Map<String, Object> user = restClient
                    .get()
                    .uri(serverUrl + "/admin/realms/" + realm + "/users/" + userId)
                    .headers(h -> h.setBearerAuth(adminToken()))
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});
            if (user == null || user.get("id") == null) {
                return new RecipientResolveResult.UserNotFound();
            }
            String email = user.get("email") == null ? null : String.valueOf(user.get("email"));
            String displayName = displayName(user);
            if (email == null || email.isBlank()) {
                return new RecipientResolveResult.NoEmail(displayName);
            }
            return new RecipientResolveResult.Found(email, displayName);
        } catch (HttpClientErrorException.NotFound e) {
            return new RecipientResolveResult.UserNotFound();
        } catch (RestClientException e) {
            log.warn("[MAIL] Resolve userId={} loi tam thuat: {}", userId, e.getMessage());
            return new RecipientResolveResult.Unavailable(e.getMessage());
        }
    }

    /** displayName = firstName + lastName, fallback username, fallback userId. */
    private static String displayName(Map<String, Object> user) {
        String first = text(user.get("firstName"));
        String last = text(user.get("lastName"));
        if (first != null && last != null) return first + " " + last;
        if (first != null) return first;
        if (last != null) return last;
        String username = text(user.get("username"));
        return username != null ? username : text(user.get("id"));
    }

    private static String text(Object v) {
        return v == null ? null : String.valueOf(v);
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
