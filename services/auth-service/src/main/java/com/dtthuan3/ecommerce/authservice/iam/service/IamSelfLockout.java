package com.dtthuan3.ecommerce.authservice.iam.service;

import com.dtthuan3.ecommerce.authservice.iam.entity.Role;
import com.dtthuan3.ecommerce.authservice.iam.entity.UserGroupRole;
import com.dtthuan3.ecommerce.authservice.iam.entity.UserRole;
import com.dtthuan3.ecommerce.authservice.iam.repository.UserGroupRoleRepository;
import com.dtthuan3.ecommerce.authservice.iam.repository.UserRoleRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Self-lockout guard: tu choi thao tac thu hoi grant DB cuoi cung giup caller giu kha nang
 * ADMIN (409). Kiem tra tren chuoi DB (direct + group-inherited); khong phu thuoc Keycloak
 * role de grant DB van phuc hoi duoc khi mapping Keycloak thay doi.
 */
@Component
@RequiredArgsConstructor
public class IamSelfLockout {

    private final UserRoleRepository userRoleRepository;
    private final UserGroupRoleRepository userGroupRoleRepository;

    /** Role duoc coi la ADMIN-capable khi code chua "ADMIN" (VD TENANT_ADMIN seed). */
    public static boolean isAdminRole(Role role) {
        return role != null && role.getCode() != null && role.getCode().toUpperCase().contains("ADMIN");
    }

    /** Tat ca role DB cua user (truc tiep + thua ke tu group hien tai). */
    public Set<Long> callerDbRoleIds(String subject, Long groupId) {
        Set<Long> ids = new HashSet<>();
        for (UserRole ur : userRoleRepository.findByUserId(subject)) {
            if (ur.getRole() != null && ur.getRole().getId() != null) {
                ids.add(ur.getRole().getId());
            }
        }
        if (groupId != null) {
            for (UserGroupRole ugr : userGroupRoleRepository.findByGroupId(groupId)) {
                if (ugr.getRole() != null && ugr.getRole().getId() != null) {
                    ids.add(ugr.getRole().getId());
                }
            }
        }
        return ids;
    }

    /**
     * Tu choi khi thu hoi {@code removedRoleId} khoi {@code targetSubject} se lay mat grant
     * ADMIN-capable DB cuoi cung cua chinh caller.
     */
    public void refuseIfStripsOwnLastAdmin(String callerSubject, Long callerGroupId, String targetSubject,
            Role removedRole, Set<Long> callerAdminRoleIdsExcludingRemoved) {
        if (!callerSubject.equals(targetSubject) || !isAdminRole(removedRole)) {
            return;
        }
        if (callerAdminRoleIdsExcludingRemoved == null || callerAdminRoleIdsExcludingRemoved.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Refused: revoking this role would strip your own last ADMIN-capable DB grant");
        }
    }

    /** Tap role ADMIN-capable trong mot tap role id cho truoc (can DB lookup). */
    public Set<Long> adminRoleIds(Set<Long> roleIds, java.util.function.Function<Long, Role> loader) {
        Set<Long> result = new HashSet<>();
        for (Long id : roleIds) {
            Role role = loader.apply(id);
            if (isAdminRole(role)) {
                result.add(id);
            }
        }
        return result;
    }

    public List<UserRole> directRoles(String subject) {
        return userRoleRepository.findByUserId(subject);
    }
}
