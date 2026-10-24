package com.dtthuan3.ecommerce.authservice.iam.service;

import com.dtthuan3.ecommerce.authservice.iam.entity.AuthUser;
import com.dtthuan3.ecommerce.authservice.iam.entity.Menu;
import com.dtthuan3.ecommerce.authservice.iam.entity.Permission;
import com.dtthuan3.ecommerce.authservice.iam.entity.UserGroupRole;
import com.dtthuan3.ecommerce.authservice.iam.entity.UserRole;
import com.dtthuan3.ecommerce.authservice.iam.repository.AuthUserRepository;
import com.dtthuan3.ecommerce.authservice.iam.repository.MenuRepository;
import com.dtthuan3.ecommerce.authservice.iam.repository.PermissionRepository;
import com.dtthuan3.ecommerce.authservice.iam.repository.UserGroupRoleRepository;
import com.dtthuan3.ecommerce.authservice.iam.repository.UserRoleRepository;
import com.dtthuan3.ecommerce.authservice.keycloak.KeycloakAdminClient;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Truy van tong hop: effective-permissions (KEYCLOAK UNION DB_DIRECT/DB_GROUP, co source tags)
 * va du lieu sider {@code GET /auth/me/menus} theo quyen.
 */
@Service
@RequiredArgsConstructor
public class IamQueryService {

    private final AuthUserRepository authUserRepository;
    private final UserRoleRepository userRoleRepository;
    private final UserGroupRoleRepository groupRoleRepository;
    private final MenuRepository menuRepository;
    private final PermissionRepository permissionRepository;
    private final KeycloakAdminClient keycloakAdmin;

    public record GrantedPermission(String code, String source) {}

    /** Hop Keycloak realm roles + DB direct + DB group-inherited, kem source tag, khong trung. */
    @Transactional(readOnly = true)
    public List<Map<String, String>> effectivePermissions(String subject) {
        Set<GrantedPermission> union = new LinkedHashSet<>();
        if (keycloakAdmin.isEnabled()) {
            try {
                for (String role : keycloakAdmin.getUserWithRoles(subject).roles()) {
                    union.add(new GrantedPermission(role, "KEYCLOAK"));
                }
            } catch (Exception ignored) {
                // Keycloak khong san sang - van tra grant DB.
            }
        }
        for (UserRole userRole : userRoleRepository.findByUserIdWithRolePermissions(subject)) {
            if (userRole.getRole() == null || userRole.getRole().getRolePermissions() == null) {
                continue;
            }
            userRole.getRole().getRolePermissions().forEach(rp -> {
                if (rp.getPermission() != null && rp.getPermission().getCode() != null) {
                    union.add(new GrantedPermission(rp.getPermission().getCode(), "DB_DIRECT"));
                }
            });
        }
        AuthUser user = authUserRepository.findById(subject).orElse(null);
        if (user != null && user.getGroup() != null) {
            for (UserGroupRole groupRole : groupRoleRepository
                    .findByGroupIdWithRolePermissions(user.getGroup().getId())) {
                if (groupRole.getRole() == null || groupRole.getRole().getRolePermissions() == null) {
                    continue;
                }
                groupRole.getRole().getRolePermissions().forEach(rp -> {
                    if (rp.getPermission() != null && rp.getPermission().getCode() != null) {
                        union.add(new GrantedPermission(rp.getPermission().getCode(), "DB_GROUP"));
                    }
                });
            }
        }
        return union.stream()
                .map(g -> Map.of("code", g.code(), "source", g.source()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Set<String> dbPermissionCodes(String subject) {
        Set<String> codes = new LinkedHashSet<>();
        for (UserRole userRole : userRoleRepository.findByUserIdWithRolePermissions(subject)) {
            if (userRole.getRole() == null || userRole.getRole().getRolePermissions() == null) {
                continue;
            }
            userRole.getRole().getRolePermissions().forEach(rp -> {
                if (rp.getPermission() != null && rp.getPermission().getCode() != null) {
                    codes.add(rp.getPermission().getCode());
                }
            });
        }
        AuthUser user = authUserRepository.findById(subject).orElse(null);
        if (user != null && user.getGroup() != null) {
            for (UserGroupRole groupRole : groupRoleRepository
                    .findByGroupIdWithRolePermissions(user.getGroup().getId())) {
                if (groupRole.getRole() == null || groupRole.getRole().getRolePermissions() == null) {
                    continue;
                }
                groupRole.getRole().getRolePermissions().forEach(rp -> {
                    if (rp.getPermission() != null && rp.getPermission().getCode() != null) {
                        codes.add(rp.getPermission().getCode());
                    }
                });
            }
        }
        return codes;
    }

    /** Tat ca permission codes (danh cho sysadmin / /me/permissions). */
    @Transactional(readOnly = true)
    public List<String> allPermissionCodes() {
        return permissionRepository.findAllByIsDeletedFalse().stream()
                .map(Permission::getCode)
                .filter(c -> c != null)
                .sorted()
                .toList();
    }

    @Transactional(readOnly = true)
    public boolean isDbSystemAdmin(String subject) {
        AuthUser user = authUserRepository.findById(subject).orElse(null);
        if (user != null && Boolean.TRUE.equals(user.getIsSystemAdmin())) {
            return true;
        }
        Set<String> roleCodes = new LinkedHashSet<>();
        for (UserRole userRole : userRoleRepository.findByUserId(subject)) {
            if (userRole.getRole() != null) {
                roleCodes.add(userRole.getRole().getCode());
            }
        }
        if (user != null && user.getGroup() != null) {
            for (UserGroupRole groupRole : groupRoleRepository.findByGroupId(user.getGroup().getId())) {
                if (groupRole.getRole() != null) {
                    roleCodes.add(groupRole.getRole().getCode());
                }
            }
        }
        return roleCodes.stream().anyMatch(c -> c != null
                && (c.equalsIgnoreCase("TENANT_ADMIN") || c.equalsIgnoreCase("ADMIN")
                        || c.equalsIgnoreCase("SYSTEM_ADMIN")));
    }

    /**
     * Du lieu sider: sysadmin thay tat ca menu active; user thuong thay menu public
     * (khong co permission nao gan vao) + menu co it nhat mot permission MENU.*
     * duoc grant. Menu co permission nhung khong duoc grant van bi an.
     * Moi entry gom code/name/path/icon/sortOrder + allowed actions (public -> []).
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> menusForUser(String subject, boolean keycloakAdmin) {
        boolean sysadmin = keycloakAdmin || isDbSystemAdmin(subject);
        Set<String> grants = sysadmin ? Set.of() : dbPermissionCodes(subject);
        List<Menu> menus = menuRepository.findAllActiveWithParent();
        List<Permission> permissions = permissionRepository.findAllByIsDeletedFalse();
        Map<Long, List<Permission>> byMenu = new LinkedHashMap<>();
        for (Permission permission : permissions) {
            if (permission.getMenu() == null || permission.getAction() == null) {
                continue;
            }
            if (!Boolean.TRUE.equals(permission.getAction().getIsActive())) {
                continue;
            }
            byMenu.computeIfAbsent(permission.getMenu().getId(), k -> new ArrayList<>()).add(permission);
        }
        Map<Long, Map<String, Object>> nodes = new TreeMap<>();
        for (Menu menu : menus) {
            List<Permission> menuPerms = byMenu.getOrDefault(menu.getId(), List.of());
            List<Map<String, Object>> allowed;
            if (sysadmin) {
                allowed = menuPerms.stream()
                        .map(p -> Map.<String, Object>of("code", p.getAction().getCode(),
                                "name", p.getAction().getName()))
                        .collect(Collectors.toList());
            } else {
                allowed = menuPerms.stream()
                        .filter(p -> grants.contains(p.getCode()))
                        .map(p -> Map.<String, Object>of("code", p.getAction().getCode(),
                                "name", p.getAction().getName()))
                        .collect(Collectors.toList());
                if (allowed.isEmpty() && !menuPerms.isEmpty()) {
                    // Menu co permission nhung caller khong duoc grant -> an.
                    continue;
                }
                // menuPerms rong -> menu public: tra ve voi actions = [].
            }
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("code", menu.getCode());
            node.put("name", menu.getName());
            node.put("path", menu.getPath());
            node.put("icon", menu.getIcon());
            node.put("sortOrder", menu.getOrderNo());
            node.put("actions", allowed);
            node.put("children", new ArrayList<Map<String, Object>>());
            nodes.put(menu.getId(), node);
        }
        List<Map<String, Object>> roots = new ArrayList<>();
        for (Menu menu : menus) {
            if (!nodes.containsKey(menu.getId())) {
                continue;
            }
            Map<String, Object> node = nodes.get(menu.getId());
            if (menu.getParent() != null && nodes.containsKey(menu.getParent().getId())) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> children =
                        (List<Map<String, Object>>) nodes.get(menu.getParent().getId()).get("children");
                children.add(node);
            } else {
                roots.add(node);
            }
        }
        return roots;
    }
}
