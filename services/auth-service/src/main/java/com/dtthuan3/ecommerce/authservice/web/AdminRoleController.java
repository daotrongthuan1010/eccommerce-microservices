package com.dtthuan3.ecommerce.authservice.web;

import com.dtthuan3.ecommerce.authservice.keycloak.KeycloakAdminClient;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Quan tri realm roles qua Keycloak Admin REST API (ADMIN only).
 *
 * <p>Membership thay doi qua {@link AdminUserController} assign/remove;
 * controller nay chi doc (list roles + members) de tranh 2 duong mutation.
 */
@RestController
@RequestMapping("/auth/admin/roles")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminRoleController {

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
}
