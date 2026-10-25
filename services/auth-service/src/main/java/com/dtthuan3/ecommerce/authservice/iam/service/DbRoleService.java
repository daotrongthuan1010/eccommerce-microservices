package com.dtthuan3.ecommerce.authservice.iam.service;

import com.dtthuan3.ecommerce.authservice.iam.entity.AuthUser;
import com.dtthuan3.ecommerce.authservice.iam.entity.Permission;
import com.dtthuan3.ecommerce.authservice.iam.entity.Role;
import com.dtthuan3.ecommerce.authservice.iam.entity.RolePermission;
import com.dtthuan3.ecommerce.authservice.iam.entity.RolePermission.RolePermissionId;
import com.dtthuan3.ecommerce.authservice.iam.entity.Tenant;
import com.dtthuan3.ecommerce.authservice.iam.entity.UserRole;
import com.dtthuan3.ecommerce.authservice.iam.entity.UserRole.UserRoleId;
import com.dtthuan3.ecommerce.authservice.iam.repository.AuthUserRepository;
import com.dtthuan3.ecommerce.authservice.iam.repository.PermissionRepository;
import com.dtthuan3.ecommerce.authservice.iam.repository.RoleRepository;
import com.dtthuan3.ecommerce.authservice.iam.repository.TenantRepository;
import com.dtthuan3.ecommerce.authservice.iam.repository.UserRoleRepository;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class DbRoleService {

    private final RoleRepository roleRepository;
    private final TenantRepository tenantRepository;
    private final PermissionRepository permissionRepository;
    private final UserRoleRepository userRoleRepository;
    private final AuthUserRepository authUserRepository;
    private final AuthContextService authContextService;
    private final IamSelfLockout selfLockout;

    private Tenant defaultTenantOrNull(Long tenantId) {
        if (tenantId != null) {
            return tenantRepository.findById(tenantId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Tenant not found: " + tenantId));
        }
        return tenantRepository.findByCode("DEFAULT").orElse(null);
    }

    @Transactional
    public Map<String, Object> create(String code, String name, String description,
            Set<Long> permissionIds, Long tenantId) {
        String upper = code.trim().toUpperCase();
        Tenant tenant = defaultTenantOrNull(tenantId);
        if (roleRepository.existsByCodeAndTenant(upper, tenant)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Role code already exists in this tenant: " + upper);
        }
        Role role = Role.builder().tenant(tenant).code(upper).name(name)
                .description(description).isSystemRole(false).build();
        role = roleRepository.save(role);
        if (permissionIds != null) {
            for (Long pid : permissionIds) {
                attachPermission(role, pid);
            }
            role = roleRepository.save(role);
        }
        authContextService.invalidateAll(List.of());
        return toMap(role);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> list(Long tenantId) {
        List<Role> roles = tenantId == null ? roleRepository.findAll()
                : roleRepository.findByTenantId(tenantId);
        return roles.stream().map(this::toMap).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> get(Long id) {
        return toMap(load(id));
    }

    @Transactional
    public void delete(String callerSubject, Long callerGroupId, Long id) {
        Role role = load(id);
        if (Boolean.TRUE.equals(role.getIsSystemRole())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot delete system role: " + role.getCode());
        }
        if (callerSubject != null && IamSelfLockout.isAdminRole(role)
                && selfLockout.callerDbRoleIds(callerSubject, callerGroupId).contains(id)) {
            Set<Long> remaining = selfLockout.callerDbRoleIds(callerSubject, callerGroupId);
            remaining.remove(id);
            Set<Long> remainingAdmin = new HashSet<>();
            for (Long rid : remaining) {
                roleRepository.findById(rid).ifPresent(r -> {
                    if (IamSelfLockout.isAdminRole(r)) {
                        remainingAdmin.add(rid);
                    }
                });
            }
            selfLockout.refuseIfStripsOwnLastAdmin(callerSubject, callerGroupId, callerSubject, role,
                    remainingAdmin);
        }
        roleRepository.delete(role);
        authContextService.invalidateAll(userIdsWithRole(id));
    }

    /** Gan role DB truc tiep cho user (theo Keycloak sub). */
    @Transactional
    public void assign(String callerSubject, String targetSubject, Long roleId) {
        Role role = load(roleId);
        AuthUser user = authUserRepository.findById(targetSubject)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Auth user not found: " + targetSubject));
        UserRoleId id = new UserRoleId(targetSubject, roleId);
        if (userRoleRepository.existsById(id)) {
            return;
        }
        UserRole userRole = UserRole.builder().id(id).user(user).role(role).assignedBy(callerSubject).build();
        userRoleRepository.save(userRole);
        authContextService.invalidate(targetSubject);
    }

    /** Thu hoi role DB truc tiep - co self-lockout guard khi caller tu thu hoi. */
    @Transactional
    public void revoke(String callerSubject, Long callerGroupId, String targetSubject, Long roleId) {
        Role role = load(roleId);
        if (callerSubject.equals(targetSubject)) {
            Set<Long> remaining = selfLockout.callerDbRoleIds(callerSubject, callerGroupId);
            remaining.remove(roleId);
            Set<Long> remainingAdmin = new HashSet<>();
            for (Long rid : remaining) {
                roleRepository.findById(rid).ifPresent(r -> {
                    if (IamSelfLockout.isAdminRole(r)) {
                        remainingAdmin.add(rid);
                    }
                });
            }
            selfLockout.refuseIfStripsOwnLastAdmin(callerSubject, callerGroupId, targetSubject, role,
                    remainingAdmin);
        }
        UserRoleId id = new UserRoleId(targetSubject, roleId);
        userRoleRepository.findById(id).ifPresent(userRoleRepository::delete);
        authContextService.invalidate(targetSubject);
    }

    /** Thay the toan bo permission cua role - co self-lockout guard. */
    @Transactional
    public Map<String, Object> setPermissions(String callerSubject, Long callerGroupId, Long roleId,
            Set<Long> permissionIds) {
        Role role = load(roleId);
        if (callerSubject != null && IamSelfLockout.isAdminRole(role)
                && selfLockout.callerDbRoleIds(callerSubject, callerGroupId).contains(roleId)
                && (permissionIds == null || permissionIds.isEmpty())) {
            Set<Long> others = selfLockout.callerDbRoleIds(callerSubject, callerGroupId);
            others.remove(roleId);
            boolean hasOtherAdmin = false;
            for (Long rid : others) {
                if (roleRepository.findById(rid).map(IamSelfLockout::isAdminRole).orElse(false)) {
                    hasOtherAdmin = true;
                    break;
                }
            }
            if (!hasOtherAdmin) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Refused: clearing permissions would strip your own last ADMIN-capable DB grant");
            }
        }
        role.getRolePermissions().clear();
        roleRepository.saveAndFlush(role);
        if (permissionIds != null) {
            for (Long pid : permissionIds) {
                attachPermission(role, pid);
            }
        }
        Role saved = roleRepository.save(role);
        authContextService.invalidateAll(userIdsWithRole(roleId));
        return toMap(saved);
    }

    private void attachPermission(Role role, Long permissionId) {
        Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Permission not found: " + permissionId));
        boolean exists = role.getRolePermissions().stream()
                .anyMatch(rp -> rp.getPermission() != null && permissionId.equals(rp.getPermission().getId()));
        if (!exists) {
            role.getRolePermissions().add(RolePermission.builder()
                    .id(new RolePermissionId(role.getId(), permissionId))
                    .role(role).permission(permission).build());
        }
    }

    private List<String> userIdsWithRole(Long roleId) {
        List<String> ids = new ArrayList<>();
        for (UserRole userRole : userRoleRepository.findByRoleId(roleId)) {
            ids.add(userRole.getUser().getId());
        }
        return ids;
    }

    private Role load(Long id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found: " + id));
    }

    public Map<String, Object> toMap(Role role) {
        List<String> codes = role.getRolePermissions() == null ? List.of()
                : role.getRolePermissions().stream()
                        .filter(rp -> rp.getPermission() != null)
                        .map(rp -> rp.getPermission().getCode()).sorted().collect(Collectors.toList());
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", role.getId());
        map.put("code", role.getCode());
        map.put("name", role.getName());
        map.put("description", role.getDescription());
        map.put("tenantId", role.getTenant() == null ? null : role.getTenant().getId());
        map.put("systemRole", Boolean.TRUE.equals(role.getIsSystemRole()));
        map.put("permissionCodes", codes);
        List<Long> permIds = role.getRolePermissions() == null ? List.of()
                : role.getRolePermissions().stream()
                        .filter(rp -> rp.getPermission() != null)
                        .map(rp -> rp.getPermission().getId()).collect(Collectors.toList());
        map.put("permissionIds", permIds);
        return map;
    }
}
