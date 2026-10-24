package com.dtthuan3.ecommerce.authservice.iam.service;

import com.dtthuan3.ecommerce.authservice.iam.entity.AuthUser;
import com.dtthuan3.ecommerce.authservice.iam.entity.Role;
import com.dtthuan3.ecommerce.authservice.iam.entity.Tenant;
import com.dtthuan3.ecommerce.authservice.iam.entity.UserGroup;
import com.dtthuan3.ecommerce.authservice.iam.entity.UserGroupRole;
import com.dtthuan3.ecommerce.authservice.iam.entity.UserGroupRole.UserGroupRoleId;
import com.dtthuan3.ecommerce.authservice.iam.repository.AuthUserRepository;
import com.dtthuan3.ecommerce.authservice.iam.repository.GroupTypeRepository;
import com.dtthuan3.ecommerce.authservice.iam.repository.PositionRepository;
import com.dtthuan3.ecommerce.authservice.iam.repository.RoleRepository;
import com.dtthuan3.ecommerce.authservice.iam.repository.TenantRepository;
import com.dtthuan3.ecommerce.authservice.iam.repository.UserGroupRepository;
import com.dtthuan3.ecommerce.authservice.iam.repository.UserGroupRoleRepository;
import java.util.ArrayList;
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
public class UserGroupAdminService {

    private final UserGroupRepository groupRepository;
    private final TenantRepository tenantRepository;
    private final GroupTypeRepository groupTypeRepository;
    private final RoleRepository roleRepository;
    private final UserGroupRoleRepository groupRoleRepository;
    private final AuthUserRepository authUserRepository;
    private final PositionRepository positionRepository;
    private final AuthContextService authContextService;
    private final IamSelfLockout selfLockout;

    @Transactional
    public Map<String, Object> create(Long tenantId, String name, String description,
            Long groupTypeId, Long parentId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Tenant not found: " + tenantId));
        UserGroup.UserGroupBuilder builder = UserGroup.builder().tenant(tenant).name(name)
                .description(description).isActive(true);
        if (groupTypeId != null) {
            builder.groupType(groupTypeRepository.findById(groupTypeId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "GroupType not found: " + groupTypeId)));
        }
        if (parentId != null) {
            builder.parent(groupRepository.findById(parentId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Parent group not found: " + parentId)));
        }
        return toMap(groupRepository.save(builder.build()));
    }

    @Transactional
    public Map<String, Object> update(Long id, String name, String description, Boolean active) {
        UserGroup group = load(id);
        if (name != null) {
            group.setName(name);
        }
        if (description != null) {
            group.setDescription(description);
        }
        if (active != null) {
            group.setIsActive(active);
        }
        return toMap(groupRepository.save(group));
    }

    @Transactional
    public void delete(Long id) {
        UserGroup group = load(id);
        if (authUserRepository.findAll().stream()
                .anyMatch(u -> u.getGroup() != null && id.equals(u.getGroup().getId()))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Group still has members, move them out first");
        }
        groupRepository.delete(group);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> list(Long tenantId) {
        List<UserGroup> groups = tenantId == null ? groupRepository.findAll()
                : groupRepository.findByTenantId(tenantId);
        return groups.stream().map(this::toMap).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> get(Long id) {
        return toMap(load(id));
    }

    /** Cay group long nhau theo parentId (loc theo tenant tu chon). */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> tree(Long tenantId) {
        List<UserGroup> groups = tenantId == null ? groupRepository.findAll()
                : groupRepository.findByTenantId(tenantId);
        Map<Long, Map<String, Object>> nodes = new LinkedHashMap<>();
        for (UserGroup group : groups) {
            Map<String, Object> node = toMap(group);
            node.put("children", new ArrayList<Map<String, Object>>());
            nodes.put(group.getId(), node);
        }
        List<Map<String, Object>> roots = new ArrayList<>();
        for (UserGroup group : groups) {
            Map<String, Object> node = nodes.get(group.getId());
            if (group.getParent() != null && nodes.containsKey(group.getParent().getId())) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> children =
                        (List<Map<String, Object>>) nodes.get(group.getParent().getId()).get("children");
                children.add(node);
            } else {
                roots.add(node);
            }
        }
        return roots;
    }

    /**
     * Groups caller duoc thay: sysadmin (DB flag) thay tat ca; nguoi thuong chi thay
     * group cua minh + cac group con.
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> accessible(String callerSubject) {
        List<UserGroup> all = groupRepository.findAll();
        AuthUser caller = authUserRepository.findById(callerSubject).orElse(null);
        if (caller != null && Boolean.TRUE.equals(caller.getIsSystemAdmin())) {
            return all.stream().map(this::toMap).collect(Collectors.toList());
        }
        Long ownGroupId = caller != null && caller.getGroup() != null ? caller.getGroup().getId() : null;
        if (ownGroupId == null) {
            return List.of();
        }
        Set<Long> visible = new java.util.HashSet<>();
        visible.add(ownGroupId);
        boolean grew = true;
        while (grew) {
            grew = false;
            for (UserGroup group : all) {
                if (group.getParent() != null && visible.contains(group.getParent().getId())
                        && visible.add(group.getId())) {
                    grew = true;
                }
            }
        }
        return all.stream().filter(g -> visible.contains(g.getId())).map(this::toMap)
                .collect(Collectors.toList());
    }

    /** Gan role cho group (endpoint moi - ca hai codebase deu chua co). */
    @Transactional
    public void assignRole(Long groupId, Long roleId) {
        UserGroup group = load(groupId);
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Role not found: " + roleId));
        UserGroupRoleId id = new UserGroupRoleId(groupId, roleId);
        if (groupRoleRepository.existsById(id)) {
            return;
        }
        groupRoleRepository.save(UserGroupRole.builder().id(id).group(group).role(role).build());
        invalidateGroupMembers(groupId);
    }

    /** Go role khoi group - co self-lockout guard. */
    @Transactional
    public void revokeRole(String callerSubject, Long callerGroupId, Long groupId, Long roleId) {
        UserGroupRoleId id = new UserGroupRoleId(groupId, roleId);
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Role not found: " + roleId));
        boolean callerInGroup = callerGroupId != null && callerGroupId.equals(groupId);
        if (callerInGroup && IamSelfLockout.isAdminRole(role)) {
            Set<Long> remaining = selfLockout.callerDbRoleIds(callerSubject, callerGroupId);
            remaining.remove(roleId);
            boolean hasOtherAdmin = false;
            for (Long rid : remaining) {
                if (roleRepository.findById(rid).map(IamSelfLockout::isAdminRole).orElse(false)) {
                    hasOtherAdmin = true;
                    break;
                }
            }
            if (!hasOtherAdmin) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Refused: revoking this group role would strip your own last ADMIN-capable DB grant");
            }
        }
        groupRoleRepository.findById(id).ifPresent(groupRoleRepository::delete);
        invalidateGroupMembers(groupId);
    }

    /** Gan user vao group (dat tenant + group + position) - co self-lockout guard. */
    @Transactional
    public void assignUser(String callerSubject, Long callerGroupId, String targetSubject,
            Long tenantId, Long groupId, Long positionId) {
        AuthUser user = authUserRepository.findById(targetSubject)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Auth user not found: " + targetSubject));
        Long oldGroupId = user.getGroup() == null ? null : user.getGroup().getId();
        boolean movesOut = oldGroupId != null && !oldGroupId.equals(groupId);
        if (callerSubject.equals(targetSubject) && movesOut) {
            Set<Long> directAdmin = new java.util.HashSet<>();
            for (var userRole : selfLockout.directRoles(callerSubject)) {
                if (userRole.getRole() != null && IamSelfLockout.isAdminRole(userRole.getRole())) {
                    directAdmin.add(userRole.getRole().getId());
                }
            }
            if (directAdmin.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Refused: moving yourself out of this group would strip your last ADMIN-capable DB grant");
            }
        }
        if (tenantId != null) {
            user.setTenant(tenantRepository.findById(tenantId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Tenant not found: " + tenantId)));
        }
        user.setGroup(groupRepository.findById(groupId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Group not found: " + groupId)));
        if (positionId != null) {
            user.setPosition(positionRepository.findById(positionId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Position not found: " + positionId)));
        }
        authUserRepository.save(user);
        authContextService.invalidate(targetSubject);
    }

    private void invalidateGroupMembers(Long groupId) {
        List<String> ids = new ArrayList<>();
        for (AuthUser user : authUserRepository.findAll()) {
            if (user.getGroup() != null && groupId.equals(user.getGroup().getId())) {
                ids.add(user.getId());
            }
        }
        authContextService.invalidateAll(ids);
    }

    private UserGroup load(Long id) {
        return groupRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Group not found: " + id));
    }

    public Map<String, Object> toMap(UserGroup group) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", group.getId());
        map.put("name", group.getName());
        map.put("description", group.getDescription());
        map.put("tenantId", group.getTenant() == null ? null : group.getTenant().getId());
        map.put("groupTypeId", group.getGroupType() == null ? null : group.getGroupType().getId());
        map.put("groupTypeCode", group.getGroupType() == null ? null : group.getGroupType().getCode());
        map.put("parentId", group.getParent() == null ? null : group.getParent().getId());
        map.put("active", Boolean.TRUE.equals(group.getIsActive()));
        // Role ids gan truc tiep cho group — FE Transfer hien thi "trong nhom".
        List<Long> roleIds = groupRoleRepository.findByGroupId(group.getId()).stream()
                .filter(gr -> gr.getRole() != null && gr.getRole().getId() != null)
                .map(gr -> gr.getRole().getId()).sorted().toList();
        map.put("roleIds", roleIds);
        return map;
    }
}
