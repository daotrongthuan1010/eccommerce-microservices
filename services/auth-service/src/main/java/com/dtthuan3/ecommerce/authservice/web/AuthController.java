package com.dtthuan3.ecommerce.authservice.web;

import com.dtthuan3.ecommerce.authservice.keycloak.KeycloakAdminClient;
import com.dtthuan3.ecommerce.authservice.security.SecurityUtils;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final RestClient restClient = RestClient.create();

    private final KeycloakAdminClient keycloakAdmin;

    @Value("${auth.keycloak.token-uri:http://162.4.177.91:7080/realms/ecommerce/protocol/openid-connect/token}")
    private String tokenUri;

    @Value("${auth.keycloak.logout-uri:http://162.4.177.91:7080/realms/ecommerce/protocol/openid-connect/logout}")
    private String logoutUri;

    @Value("${auth.keycloak.client-id:ecommerce-app}")
    private String clientId;

    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> login(@Valid @RequestBody LoginRequest request) {
        // Ghi chu P2: giu password grant proxy (khong chuyen Authorization Code + PKCE)
        // cho den khi co frontend that dam nhan redirect flow.
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("client_id", clientId);
        form.add("username", request.username());
        form.add("password", request.password());
        return postTokenForm(form);
    }

    @PostMapping(value = "/refresh", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> refresh(@Valid @RequestBody RefreshRequest request) {
        // Chuan Keycloak public client: grant_type=refresh_token + client_id, khong can secret.
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "refresh_token");
        form.add("client_id", clientId);
        form.add("refresh_token", request.refreshToken());
        return postTokenForm(form);
    }

    @PostMapping(value = "/logout", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> logout(@Valid @RequestBody LogoutRequest request) {
        // Best-effort: du refresh token co het han / da revoke thi van tra thanh cong
        // de client xoa token local va dieu huong ve login.
        // Chuan Keycloak RP-Initiated logout cho public client: client_id + refresh_token
        // (id_token_hint la phuong an thay the khi client co id_token).
        try {
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("client_id", clientId);
            if (request.idTokenHint() != null && !request.idTokenHint().isBlank()) {
                form.add("id_token_hint", request.idTokenHint());
            }
            form.add("refresh_token", request.refreshToken());
            restClient
                    .post()
                    .uri(logoutUri)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception ex) {
            log.warn("Keycloak logout proxy failed (best-effort): {}", ex.getMessage());
        }
        return Map.of("logout", true);
    }

    /**
     * Dang ky tai khoan moi qua Keycloak Admin API (realm DEV co registrationAllowed=false
     * nen khong self-register bang API cong khai). Can cau hinh service account
     * (auth.keycloak.admin.*); chua cau hinh -> 501 kem huong dan thu cong.
     */
    @PostMapping(value = "/register", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody RegisterRequest request) {
        keycloakAdmin.requireEnabled();
        String userId = keycloakAdmin.createUser(
                request.username(), request.email(), request.firstName(), request.lastName(), request.password());
        // Gan role USER mac dinh cho tai khoan moi tu dang ky (best-effort: khong chan dang ky).
        keycloakAdmin.assignRealmRoleBestEffort(userId, "USER");
        log.info("Registered new Keycloak user {} ({})", request.username(), userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("userId", userId, "username", request.username(), "email", request.email()));
    }

    /**
     * Quen mat khau: gui email dat lai mat khau qua Keycloak (UPDATE_PASSWORD).
     * Luon tra {sent:true} de khong lo thong tin user co ton tai hay khong.
     */
    @PostMapping(value = "/forgot-password", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        keycloakAdmin.requireEnabled();
        String userId = keycloakAdmin.findUserId(request.username());
        if (userId == null) {
            log.info("Forgot-password requested for unknown username (masked response)");
            return Map.of("sent", true);
        }
        try {
            keycloakAdmin.sendRequiredActionEmail(userId, "UPDATE_PASSWORD");
        } catch (Exception ex) {
            // Keycloak nem 500 khi realm chua cau hinh SMTP - log ro de DEV biet cai dat mail.
            log.warn("Send reset-password email failed for user {}: {}", request.username(), ex.getMessage());
            throw new MailNotConfiguredException();
        }
        return Map.of("sent", true);
    }

    /**
     * Doi mat khau khi da login: xac thuc mat khau cu bang password grant truoc,
     * roi reset mat khau qua Admin API. Sai mat khau cu -> 400.
     */
    @PostMapping(value = "/change-password", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public Map<String, Object> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        var user = SecurityUtils.requireUser();
        keycloakAdmin.requireEnabled();
        // Xac thuc mat khau cu: dung password grant, sai -> Keycloak 401 -> map thanh 400.
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("client_id", clientId);
        form.add("username", user.username());
        form.add("password", request.currentPassword());
        try {
            postTokenForm(form);
        } catch (org.springframework.web.client.RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 400 || ex.getStatusCode().value() == 401) {
                throw new WrongCurrentPasswordException();
            }
            throw ex;
        }
        keycloakAdmin.resetPassword(user.subject(), request.newPassword());
        return Map.of("changed", true);
    }

    /** Gui lai email xac thuc cho user da login (VERIFY_EMAIL). */
    @PostMapping(value = "/send-verify-email", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public Map<String, Object> sendVerifyEmail() {
        var user = SecurityUtils.requireUser();
        keycloakAdmin.requireEnabled();
        try {
            keycloakAdmin.sendRequiredActionEmail(user.subject(), "VERIFY_EMAIL");
        } catch (Exception ex) {
            log.warn("Send verify email failed for user {}: {}", user.username(), ex.getMessage());
            throw new MailNotConfiguredException();
        }
        return Map.of("sent", true);
    }

    /**
     * Profile hien tai (thong nhat qua gateway: GET /api/auth/me).
     * Cung shape voi user-service /me de client dung 1 cach doc.
     * Gateway route auth-service StripPrefix=1 nen giu prefix /auth o day
     * (FE SSO bat buoc can endpoint nay sau khi doi token).
     *
     * <p>Giau profile tu Keycloak Admin API (firstName/lastName/picture attribute)
     * khi service account cau hinh san; chua cau hinh -> fallback ve JWT claims
     * de FE van hien thi duoc ten/email co ban.
     */
    @GetMapping(value = "/me", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public Map<String, Object> me() {
        var user = SecurityUtils.requireUser();
        Map<String, Object> profile = new java.util.LinkedHashMap<>();
        profile.put("subject", String.valueOf(user.subject()));
        profile.put("username", String.valueOf(user.username()));
        profile.put("email", String.valueOf(user.email()));
        profile.put("roles", user.roles() == null ? List.of() : user.roles());
        profile.put("admin", user.isAdmin());
        if (keycloakAdmin.isEnabled()) {
            try {
                var admin = keycloakAdmin.getUserWithRoles(user.subject());
                if (admin.firstName() != null) {
                    profile.put("firstName", admin.firstName());
                }
                if (admin.lastName() != null) {
                    profile.put("lastName", admin.lastName());
                }
                if (admin.email() != null) {
                    profile.put("email", admin.email());
                }
                profile.put("emailVerified", admin.emailVerified());
                if (admin.picture() != null) {
                    profile.put("picture", admin.picture());
                }
            } catch (Exception ex) {
                log.warn("Enrich /me from Keycloak failed for {} (fallback to JWT claims): {}",
                        user.username(), ex.getMessage());
            }
        }
        return profile;
    }

    /**
     * Tu sua profile cua chinh minh (firstName/lastName/email).
     * Keycloak la identity source duy nhat - khong co DB profile song song.
     */
    @PutMapping(value = "/me/profile", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public Map<String, Object> updateMyProfile(@Valid @RequestBody UpdateProfileRequest request) {
        var user = SecurityUtils.requireUser();
        keycloakAdmin.requireEnabled();
        var updated = keycloakAdmin.updateUserProfile(
                user.subject(), request.firstName(), request.lastName(), request.email());
        log.info("User {} updated own profile", user.username());
        return profileOf(updated);
    }

    /**
     * Luu file key anh dai dien cua chinh minh vao attribute Keycloak
     * {@code picture}. FE upload file truoc qua file-service
     * ({@code POST /api/file/files} multipart) roi gui key ve day.
     * Chi luu key, KHONG luu presigned URL - FE resolve URL moi lan hien thi
     * qua file-service {@code GET /files/url?key=...}.
     * fileKey null/blank = xoa avatar (doi xung voi admin PATCH /{id}/avatar).
     */
    @PutMapping(value = "/me/avatar", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public Map<String, Object> updateMyAvatar(@Valid @RequestBody UpdateAvatarRequest request) {
        var user = SecurityUtils.requireUser();
        keycloakAdmin.requireEnabled();
        var updated = keycloakAdmin.setUserPicture(user.subject(), request.fileKey());
        log.info("User {} updated own avatar", user.username());
        return profileOf(updated);
    }

    private static Map<String, Object> profileOf(KeycloakAdminClient.AdminUser admin) {
        Map<String, Object> profile = new java.util.LinkedHashMap<>();
        profile.put("subject", admin.id());
        profile.put("username", admin.username());
        profile.put("email", admin.email());
        profile.put("firstName", admin.firstName());
        profile.put("lastName", admin.lastName());
        profile.put("emailVerified", admin.emailVerified());
        profile.put("roles", admin.roles() == null ? List.of() : admin.roles());
        profile.put("admin", admin.roles() != null && admin.roles().stream().anyMatch("ADMIN"::equalsIgnoreCase));
        profile.put("picture", admin.picture());
        return profile;
    }

    private Map<String, Object> postTokenForm(MultiValueMap<String, String> form) {
        Map<?, ?> token = restClient
                .post()
                .uri(tokenUri)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(Map.class);
        if (token == null) {
            return Map.of();
        }
        return Map.<String, Object>copyOf((Map<String, Object>) (Map<?, ?>) token);
    }

    public static class WrongCurrentPasswordException extends RuntimeException {
        public WrongCurrentPasswordException() {
            super("Current password is incorrect");
        }
    }

    public record UpdateProfileRequest(
            @Size(max = 64, message = "firstName must be at most 64 chars") String firstName,
            @Size(max = 64, message = "lastName must be at most 64 chars") String lastName,
            @Email(message = "email is invalid") @Size(max = 254) String email) {}

    public record UpdateAvatarRequest(
            @Size(max = 512, message = "fileKey must be at most 512 chars")
                    @Pattern(regexp = "^[A-Za-z0-9._/\\-]*$",
                            message = "fileKey has invalid characters")
                    String fileKey) {}

    public static class MailNotConfiguredException extends RuntimeException {
        public MailNotConfiguredException() {
            super("Keycloak SMTP is not configured, cannot send email");
        }
    }
}
