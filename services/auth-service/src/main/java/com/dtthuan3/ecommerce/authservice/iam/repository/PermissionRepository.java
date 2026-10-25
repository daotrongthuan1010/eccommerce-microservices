package com.dtthuan3.ecommerce.authservice.iam.repository;

import com.dtthuan3.ecommerce.authservice.iam.entity.Permission;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PermissionRepository extends JpaRepository<Permission, Long> {

    Optional<Permission> findByCode(String code);

    boolean existsByCode(String code);

    List<Permission> findAllByIsDeletedFalse();

    List<Permission> findByMenuIdAndIsDeletedFalse(Long menuId);
}
