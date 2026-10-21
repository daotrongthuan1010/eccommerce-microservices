package com.dtthuan3.ecommerce.authservice.iam.dto;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** AuthContext - cache trong Redis, gateway lay qua API internal de ra quyet dinh phan quyen. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthContext implements Serializable {
    private static final long serialVersionUID = 1L;

    private String userId;
    private String username;
    private String fullName;
    private String email;
    private Boolean isSystemAdmin;

    private Long tenantId;
    private String tenantCode;
    private String tenantName;

    private Long groupId;
    private String groupName;
    private String groupTypeCode;
    private String groupTypeName;
    private Long parentGroupId;

    private Long positionId;
    private String positionName;
    private Integer positionLevel;

    @Builder.Default
    private Set<String> roleCodes = new HashSet<>();

    @Builder.Default
    private Set<String> permissions = new HashSet<>();

    public boolean hasPermission(String permissionCode) {
        return permissions != null && permissions.contains(permissionCode);
    }

    public boolean hasRole(String roleCode) {
        return roleCodes != null && roleCodes.contains(roleCode);
    }

    public boolean isSystemAdmin() {
        return Boolean.TRUE.equals(isSystemAdmin);
    }

    public boolean hasPositionLevel(int minLevel) {
        return positionLevel != null && positionLevel >= minLevel;
    }

    public boolean isSameTenant(Long otherTenantId) {
        return tenantId != null && tenantId.equals(otherTenantId);
    }

    public boolean isGroupType(String typeCode) {
        return groupTypeCode != null && groupTypeCode.equalsIgnoreCase(typeCode);
    }

    public boolean isSameGroup(Long otherGroupId) {
        return groupId != null && groupId.equals(otherGroupId);
    }

    public boolean canActOnTenant(String permissionCode, Long resourceTenantId) {
        if (Boolean.TRUE.equals(isSystemAdmin)) {
            return true;
        }
        return hasPermission(permissionCode) && isSameTenant(resourceTenantId);
    }
}
