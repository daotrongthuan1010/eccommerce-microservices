package com.dtthuan3.ecommerce.authservice.web;

import com.dtthuan3.ecommerce.authservice.keycloak.KeycloakAdminClient;
import com.dtthuan3.ecommerce.authservice.security.SecurityUtils;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Quan tri user qua Keycloak Admin REST API (ADMIN only).
 *
 * <p>Khong dung IAM DB (@Deprecated dead path theo docs/RBAC-MODEL.md) -
 * moi doc/ghi deu qua {@link KeycloakAdminClient} voi service-account token.
 * Tu choi tu-khoa (xoa ADMIN / disable chinh minh) voi 400 de tranh self-lockout.
 */
@RestController
@RequestMapping("/auth/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminUserController {

    private static final Logger log = LoggerFactory.getLogger(AdminUserController.class);

    private final KeycloakAdminClient keycloakAdmin;

    @GetMapping
    public Map<String, Object> list(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int safeSize = Math.min(Math.max(size, 1), 50);
        int safePage = Math.max(page, 0);
        List<KeycloakAdminClient.AdminUser> items =
                keycloakAdmin.listUsers(search, safePage * safeSize, safeSize);
        long total = keycloakAdmin.countUsers(search);
        if (total < 0) {
            total = (long) safePage * safeSize + items.size();
        }
        return Map.of("items", items, "total", total, "page", safePage, "size", safeSize);
    }

    @GetMapping("/{id}")
    public KeycloakAdminClient.AdminUser detail(@PathVariable String id) {
        return keycloakAdmin.getUserWithRoles(id);
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@Valid @RequestBody CreateUserRequest request) {
        List<String> roles = request.roles() == null || request.roles().isEmpty()
                ? List.of("USER")
                : List.copyOf(request.roles());
        // Fail-fast: role la -> 404 truoc khi tao user.
        for (String role : roles) {
            keycloakAdmin.getRealmRole(role);
        }
        String userId = keycloakAdmin.createUser(
                request.username(), request.email(),
                request.firstName(), request.lastName(), request.password());
        for (String role : roles) {
            keycloakAdmin.assignRealmRole(userId, role);
        }
        if (request.sendVerifyEmail()) {
            try {
                keycloakAdmin.sendRequiredActionEmail(userId, "VERIFY_EMAIL");
            } catch (Exception ex) {
                log.warn("Send verify email to new user {} failed (best-effort): {}", userId, ex.getMessage());
            }
        }
        log.info("Admin created Keycloak user {} ({}) with roles {}", request.username(), userId, roles);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of(
                        "id", userId,
                        "userId", userId,
                        "username", request.username(),
                        "roles", roles));
    }

    @PostMapping("/{id}/roles")
    public Map<String, Object> assignRole(
            @PathVariable String id, @Valid @RequestBody RoleAssignRequest request) {
        keycloakAdmin.getRealmRole(request.role());
        keycloakAdmin.assignRealmRole(id, request.role());
        return Map.of("userId", id, "role", request.role(), "assigned", true);
    }

    @DeleteMapping("/{id}/roles/{role}")
    public Map<String, Object> removeRole(@PathVariable String id, @PathVariable String role) {
        String selfId = SecurityUtils.requireUser().subject();
        if (id.equals(selfId) && "ADMIN".equalsIgnoreCase(role)) {
            throw new SelfLockoutException("Cannot remove ADMIN role from your own account");
        }
        keycloakAdmin.removeRealmRole(id, role);
        return Map.of("userId", id, "role", role, "removed", true);
    }

    @PatchMapping("/{id}/enabled")
    public Map<String, Object> setEnabled(
            @PathVariable String id, @Valid @RequestBody EnabledRequest request) {
        String selfId = SecurityUtils.requireUser().subject();
        if (id.equals(selfId) && Boolean.FALSE.equals(request.enabled())) {
            throw new SelfLockoutException("Cannot disable your own account");
        }
        keycloakAdmin.setEnabled(id, request.enabled());
        return Map.of("userId", id, "enabled", request.enabled());
    }

    @PostMapping("/{id}/reset-password")
    public Map<String, Object> resetPassword(
            @PathVariable String id, @Valid @RequestBody ResetPasswordRequest request) {
        keycloakAdmin.resetPassword(id, request.newPassword(), request.temporary());
        if (request.temporary()) {
            try {
                keycloakAdmin.addRequiredAction(id, "UPDATE_PASSWORD");
            } catch (Exception ex) {
                log.warn("Add UPDATE_PASSWORD action for user {} failed (best-effort): {}",
                        id, ex.getMessage());
            }
        }
        return Map.of("userId", id, "reset", true);
    }

    public static class SelfLockoutException extends RuntimeException {
        public SelfLockoutException(String message) {
            super(message);
        }
    }

    public record CreateUserRequest(
            @NotBlank(message = "username is required")
                    @Size(min = 3, max = 64, message = "username must be 3-64 chars")
                    @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "username has invalid characters")
                    String username,
            @NotBlank(message = "email is required") @Email(message = "email is invalid") String email,
            @NotBlank(message = "password is required")
                    @Size(min = 8, max = 128, message = "password must be at least 8 chars")
                    String password,
            @Size(max = 64) String firstName,
            @Size(max = 64) String lastName,
            List<String> roles,
            boolean sendVerifyEmail) {}

    public record RoleAssignRequest(@NotBlank(message = "role is required") String role) {}

    public record EnabledRequest(@NotNull(message = "enabled is required") Boolean enabled) {}

    public record ResetPasswordRequest(
            @NotBlank(message = "newPassword is required")
                    @Size(min = 8, max = 128, message = "newPassword must be at least 8 chars")
                    String newPassword,
            boolean temporary) {}
}
