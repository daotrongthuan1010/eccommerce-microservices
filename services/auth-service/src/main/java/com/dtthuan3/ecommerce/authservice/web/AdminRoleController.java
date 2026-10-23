package com.dtthuan3.ecommerce.authservice.web;

import com.dtthuan3.ecommerce.authservice.keycloak.KeycloakAdminClient;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Quan tri realm roles qua Keycloak Admin REST API (ADMIN only).
 *
 * <p>Membership thay doi qua {@link AdminUserController} assign/remove.
 * Tranches hien tai (Keycloak-only P0): controller ho tro tao/xoa realm role
 * tu dinh nghia; permission/resource chi tiet kieu DB (smart-contract) la
 * tranche B se thay the (xem docs/RBAC-MODEL.md).
 */
@RestController
@RequestMapping("/auth/admin/roles")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminRoleController {

    private static final Logger log = LoggerFactory.getLogger(AdminRoleController.class);

    /** Role he thong cua Keycloak - khong cho xoa/tao trung qua API nay. */
    private static final Set<String> PROTECTED_ROLES = Set.of(
            "offline_access", "uma_authorization", "default-roles-ecommerce", "default-roles-master");

    /**
     * Role nghiep vu cot loi - xoa se tuot het membership nen BE tu choi
     * 400 ke ca khi FE da an nut (API bypassable). So sanh case-insensitive
     * vi Keycloak Admin Console hay viet hoa (ADMIN/USER).
     */
    private static boolean isProtectedRole(String role) {
        if (role == null) {
            return true;
        }
        return PROTECTED_ROLES.contains(role)
                || "ADMIN".equalsIgnoreCase(role)
                || "USER".equalsIgnoreCase(role);
    }

    private final KeycloakAdminClient keycloakAdmin;

    @GetMapping
    public Map<String, Object> list() {
        List<KeycloakAdminClient.RealmRole> roles = keycloakAdmin.listRealmRoles();
        return Map.of("items", roles, "total", roles.size());
    }

    @GetMapping("/{role}/users")
    public Map<String, Object> members(@PathVariable String role) {
        List<KeycloakAdminClient.AdminUser> users = keycloakAdmin.listUsersInRole(role);
        return Map.of("role", role, "items", users, "total", users.size());
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@Valid @RequestBody CreateRoleRequest request) {
        String name = request.name().trim();
        if (isProtectedRole(name)) {
            throw new ProtectedRoleException(name);
        }
        keycloakAdmin.createRealmRole(name, request.description());
        log.info("Admin created realm role {}", name);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("name", name, "created", true));
    }

    @DeleteMapping("/{role}")
    public Map<String, Object> delete(@PathVariable String role) {
        if (isProtectedRole(role)) {
            throw new ProtectedRoleException(role);
        }
        keycloakAdmin.deleteRealmRole(role);
        log.info("Admin deleted realm role {}", role);
        return Map.of("role", role, "deleted", true);
    }

    public static class ProtectedRoleException extends RuntimeException {
        public ProtectedRoleException(String role) {
            super("Role is protected and cannot be deleted: " + role);
        }
    }

    public record CreateRoleRequest(
            @NotBlank(message = "name is required")
                    @Size(min = 2, max = 64, message = "name must be 2-64 chars")
                    @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "name has invalid characters")
                    String name,
            @Size(max = 255, message = "description must be at most 255 chars") String description) {}
}
