package com.dtthuan3.ecommerce.authservice.web;

import com.dtthuan3.ecommerce.authservice.keycloak.KeycloakAdminClient;
import com.dtthuan3.ecommerce.authservice.security.CurrentUser;
import com.dtthuan3.ecommerce.authservice.security.SecurityUtils;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Chuoi doanh nghiep: cap tren tao tai khoan cho cap duoi (khac self-register
 * /auth/register). ADMIN tao MODERATOR; MODERATOR tao USER. Moi nguoi chi thay
 * va quan ly cap duoi TRUC TIEP cua minh (attribute managerId) - khong de quy.
 *
 * <p>Role-grant matrix (chong leo thang quyen): ADMIN -> {USER, MODERATOR};
 * MODERATOR -> {USER}. Khong ai tu tao ADMIN.
 */
@RestController
@RequestMapping("/auth/staff/users")
@PreAuthorize("hasAnyRole('ADMIN','MODERATOR')")
@RequiredArgsConstructor
public class StaffUserController {

    private static final Logger log = LoggerFactory.getLogger(StaffUserController.class);

    /** Role moi cap duoc gan cho cap duoi - cap duoi luon thap hon cap tao. */
    private static final Map<String, Set<String>> GRANTABLE_ROLES = Map.of(
            "ADMIN", Set.of("USER", "MODERATOR"),
            "MODERATOR", Set.of("USER"));

    private final KeycloakAdminClient keycloakAdmin;

    /** Danh sach cap duoi truc tiep cua toi (managerId = subject cua toi). */
    @GetMapping
    public Map<String, Object> myStaff() {
        var me = SecurityUtils.requireUser();
        List<KeycloakAdminClient.AdminUser> staff = keycloakAdmin.listDirectSubordinates(me.subject());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("items", staff);
        body.put("total", staff.size());
        return body;
    }

    /** Chi tiet 1 cap duoi truc tiep - chi xem duoc nguoi minh tao. */
    @GetMapping("/{id}")
    public KeycloakAdminClient.AdminUser detail(@PathVariable String id) {
        var me = SecurityUtils.requireUser();
        KeycloakAdminClient.AdminUser user = keycloakAdmin.requireUser(id);
        if (!me.subject().equals(user.managerId())) {
            throw new NotMySubordinateException(id);
        }
        return user;
    }

    /**
     * Tao tai khoan cho cap duoi. Tai khoan doanh nghiep duoc TIN CUOC tu
     * nguoi tao (emailVerified=true khi khong gui mail verify) nen login duoc
     * ngay; sendVerifyEmail=true thi user phai xac thuc email truoc.
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@Valid @RequestBody CreateStaffRequest request) {
        var me = SecurityUtils.requireUser();
        keycloakAdmin.requireEnabled();
        List<String> roles = request.roles() == null || request.roles().isEmpty()
                ? List.of("USER")
                : List.copyOf(request.roles());
        // Role matrix: chi cho gan role thuoc cap cua nguoi tao.
        Set<String> grantable = grantableFor(me);
        for (String role : roles) {
            if (!grantable.contains(role.toUpperCase())) {
                throw new RoleNotGrantableException(me.roles(), role);
            }
        }
        for (String role : roles) {
            keycloakAdmin.getRealmRole(role); // fail-fast 404
        }
        // accountType + managerId gan ngay luc tao -> khong co user "mo coi".
        String userId = keycloakAdmin.createUserWithAttributes(
                request.username(), request.email(),
                request.firstName(), request.lastName(), request.password(),
                Map.of(
                        "accountType", List.of("ENTERPRISE_STAFF"),
                        "managerId", List.of(me.subject())));
        for (String role : roles) {
            keycloakAdmin.assignRealmRoleBestEffort(userId, role);
        }
        if (Boolean.TRUE.equals(request.sendVerifyEmail())) {
            try {
                keycloakAdmin.sendRequiredActionEmail(userId, "VERIFY_EMAIL");
            } catch (Exception ex) {
                log.warn("Send VERIFY_EMAIL to new staff {} failed (best-effort): {}",
                        userId, ex.getMessage());
            }
        } else {
            // Cap tren tin cuoc email cong ty -> danh dau verified de login duoc ngay.
            keycloakAdmin.setUserAttributes(userId, Map.of("emailVerified", "true"));
        }
        log.info("Staff {} created subordinate {} ({}) roles={}",
                me.username(), request.username(), userId, roles);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", userId);
        body.put("userId", userId);
        body.put("username", request.username());
        body.put("roles", roles);
        body.put("managerId", me.subject());
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    /** Khoa/mo 1 cap duoi truc tiep (khong the khoa chinh minh - da la cap tren). */
    @PostMapping("/{id}/enabled")
    public Map<String, Object> setEnabled(@PathVariable String id, @Valid @RequestBody EnabledRequest request) {
        var me = SecurityUtils.requireUser();
        KeycloakAdminClient.AdminUser user = keycloakAdmin.requireUser(id);
        if (!me.subject().equals(user.managerId())) {
            throw new NotMySubordinateException(id);
        }
        keycloakAdmin.setEnabled(id, request.enabled());
        log.info("Staff {} {} subordinate {}", me.username(),
                Boolean.TRUE.equals(request.enabled()) ? "enabled" : "disabled", id);
        return Map.of("userId", id, "enabled", request.enabled());
    }

    /** Reset mat khau cho cap duoi truc tiep. temporary=true -> user doi mat khau lan dau. */
    @PostMapping("/{id}/reset-password")
    public Map<String, Object> resetPassword(
            @PathVariable String id, @Valid @RequestBody ResetPasswordRequest request) {
        var me = SecurityUtils.requireUser();
        KeycloakAdminClient.AdminUser user = keycloakAdmin.requireUser(id);
        if (!me.subject().equals(user.managerId())) {
            throw new NotMySubordinateException(id);
        }
        keycloakAdmin.resetPassword(id, request.newPassword(), request.temporary());
        if (request.temporary()) {
            try {
                keycloakAdmin.addRequiredAction(id, "UPDATE_PASSWORD");
            } catch (Exception ex) {
                log.warn("Add UPDATE_PASSWORD action for staff {} failed (best-effort): {}",
                        id, ex.getMessage());
            }
        }
        return Map.of("userId", id, "reset", true);
    }

    private static Set<String> grantableFor(CurrentUser me) {
        Set<String> grantable = GRANTABLE_ROLES.get(me.highestRole());
        if (grantable == null) {
            throw new RoleNotGrantableException(me.roles(), null);
        }
        return grantable;
    }

    public static class NotMySubordinateException extends RuntimeException {
        public NotMySubordinateException(String userId) {
            super("User is not your direct subordinate: " + userId);
        }
    }

    public static class RoleNotGrantableException extends RuntimeException {
        public RoleNotGrantableException(java.util.Collection<String> myRoles, String requestedRole) {
            super("Your role " + myRoles + " cannot grant role: " + requestedRole);
        }
    }

    public record CreateStaffRequest(
            @NotBlank(message = "username is required")
                    @Size(min = 3, max = 64, message = "username must be 3-64 chars")
                    @jakarta.validation.constraints.Pattern(regexp = "^[a-zA-Z0-9._-]+$",
                            message = "username has invalid characters")
                    String username,
            @NotBlank(message = "email is required") @Email(message = "email is invalid") String email,
            @NotBlank(message = "password is required")
                    @Size(min = 8, max = 128, message = "password must be at least 8 chars")
                    String password,
            @Size(max = 64) String firstName,
            @Size(max = 64) String lastName,
            List<String> roles,
            /** true = gui email xac thuc; false (mac dinh) = cap tren tin cuoc, login ngay. */
            Boolean sendVerifyEmail) {}

    public record EnabledRequest(Boolean enabled) {}

    public record ResetPasswordRequest(
            @NotBlank(message = "newPassword is required")
                    @Size(min = 8, max = 128, message = "newPassword must be at least 8 chars")
                    String newPassword,
            boolean temporary) {}
}
