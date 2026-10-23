package com.dtthuan3.ecommerce.authservice.web;

import com.dtthuan3.ecommerce.authservice.keycloak.KeycloakAdminClient;
import com.dtthuan3.ecommerce.authservice.keycloak.KeycloakAdminDisabledException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientResponseException;

/**
 * Dich loi Keycloak / validation thanh HTTP sach cho client thay vi 500 trang.
 *
 * <ul>
 *   <li>Loi 400/401 tu Keycloak token endpoint (sai password, refresh het han) -> giu
 *       nguyen status, boc error_description de client hien thi.</li>
 *   <li>Loi con lai tu Keycloak -> 502 (backend phu thuoc hong).</li>
 *   <li>Thieu validation DTO -> 400 kem chi tiet field.</li>
 * </ul>
 */
@RestControllerAdvice
public class AuthExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(AuthExceptionHandler.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    @ExceptionHandler(RestClientResponseException.class)
    public ResponseEntity<Map<String, Object>> handleKeycloakError(RestClientResponseException ex) {
        int status = ex.getStatusCode().value();
        String description = extractKeycloakDescription(ex.getResponseBodyAsString());
        log.warn("Keycloak proxy error status={} error={}", status, description);
        if (status == HttpStatus.BAD_REQUEST.value() || status == HttpStatus.UNAUTHORIZED.value()) {
            // invalid_grant (sai password / refresh het han), invalid_request, unauthorized_client...
            return ResponseEntity.status(status)
                    .body(Map.of("error", "invalid_grant", "error_description", description));
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", "keycloak_unavailable");
        body.put("error_description", description);
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fields = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(e -> fields.put(e.getField(), e.getDefaultMessage()));
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", "validation_failed");
        body.put("fields", fields);
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(KeycloakAdminDisabledException.class)
    public ResponseEntity<Map<String, Object>> handleAdminDisabled(KeycloakAdminDisabledException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", "registration_not_configured");
        body.put(
                "message",
                "Chua cau hinh Keycloak service account (auth.keycloak.admin.client-id/client-secret). "
                        + "Thu cong: tao user trong Keycloak Admin Console (Users > Add user, dat password non-temporary, "
                        + "gan realm role USER), hoac tao confidential client co service account va dat secret qua env "
                        + "KEYCLOAK_ADMIN_CLIENT_ID / KEYCLOAK_ADMIN_CLIENT_SECRET.");
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(body);
    }

    @ExceptionHandler(KeycloakAdminClient.UserAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleUserExists(KeycloakAdminClient.UserAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", "user_exists", "message", ex.getMessage()));
    }

    @ExceptionHandler(KeycloakAdminClient.UserNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleUserNotFound(KeycloakAdminClient.UserNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "user_not_found", "message", ex.getMessage()));
    }

    @ExceptionHandler(KeycloakAdminClient.RealmRoleNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleRoleNotFound(
            KeycloakAdminClient.RealmRoleNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "role_not_found", "message", ex.getMessage()));
    }

    @ExceptionHandler(AdminUserController.SelfLockoutException.class)
    public ResponseEntity<Map<String, Object>> handleSelfLockout(AdminUserController.SelfLockoutException ex) {
        return ResponseEntity.badRequest()
                .body(Map.of("error", "self_lockout", "message", ex.getMessage()));
    }

    @ExceptionHandler(AuthController.WrongCurrentPasswordException.class)
    public ResponseEntity<Map<String, Object>> handleWrongPassword(AuthController.WrongCurrentPasswordException ex) {
        return ResponseEntity.badRequest()
                .body(Map.of("error", "wrong_password", "message", ex.getMessage()));
    }

    @ExceptionHandler(AuthController.MailNotConfiguredException.class)
    public ResponseEntity<Map<String, Object>> handleMailNotConfigured(AuthController.MailNotConfiguredException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", "mail_not_configured");
        body.put(
                "message",
                "Keycloak realm chua cau hinh SMTP nen khong gui duoc email. "
                        + "Thu cong: Realm Settings > Email, dien SMTP host/port/user/password, hoac doi mat khau "
                        + "truc tiep trong Keycloak Admin Console (Users > Credentials > Reset password).");
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(body);
    }

    private String extractKeycloakDescription(String body) {
        if (body == null || body.isBlank()) {
            return "Keycloak request failed";
        }
        try {
            Map<?, ?> map = objectMapper.readValue(body, Map.class);
            Object desc = map.get("error_description");
            if (desc != null && !String.valueOf(desc).isBlank()) {
                return String.valueOf(desc);
            }
            Object err = map.get("errorMessage");
            if (err != null && !String.valueOf(err).isBlank()) {
                return String.valueOf(err);
            }
        } catch (Exception ignored) {
            // body khong phai JSON - tra nguyen van (cat ngan)
        }
        return body.length() > 300 ? body.substring(0, 300) : body;
    }
}
