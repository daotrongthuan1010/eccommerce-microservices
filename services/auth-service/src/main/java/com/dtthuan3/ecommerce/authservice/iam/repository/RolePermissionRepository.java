package com.dtthuan3.ecommerce.authservice.iam.repository;

import com.dtthuan3.ecommerce.authservice.iam.entity.RolePermission;
import com.dtthuan3.ecommerce.authservice.iam.entity.RolePermission.RolePermissionId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RolePermissionRepository extends JpaRepository<RolePermission, RolePermissionId> {

    List<RolePermission> findByRoleId(Long roleId);

    void deleteByRoleIdAndPermissionId(Long roleId, Long permissionId);
}
