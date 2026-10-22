package com.dtthuan3.ecommerce.authservice.iam.repository;

import com.dtthuan3.ecommerce.authservice.iam.entity.UserRole;
import com.dtthuan3.ecommerce.authservice.iam.entity.UserRole.UserRoleId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRoleRepository extends JpaRepository<UserRole, UserRoleId> {

    @Query("select ur from UserRole ur join fetch ur.role r left join fetch r.rolePermissions rp"
            + " left join fetch rp.permission where ur.user.id = :userId")
    List<UserRole> findByUserIdWithRolePermissions(@Param("userId") String userId);
}
