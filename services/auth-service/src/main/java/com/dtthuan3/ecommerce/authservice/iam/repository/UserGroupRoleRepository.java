package com.dtthuan3.ecommerce.authservice.iam.repository;

import com.dtthuan3.ecommerce.authservice.iam.entity.UserGroupRole;
import com.dtthuan3.ecommerce.authservice.iam.entity.UserGroupRole.UserGroupRoleId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserGroupRoleRepository extends JpaRepository<UserGroupRole, UserGroupRoleId> {

    @Query("select ugr from UserGroupRole ugr join fetch ugr.role r left join fetch r.rolePermissions rp"
            + " left join fetch rp.permission where ugr.group.id = :groupId")
    List<UserGroupRole> findByGroupIdWithRolePermissions(@Param("groupId") Long groupId);
}
