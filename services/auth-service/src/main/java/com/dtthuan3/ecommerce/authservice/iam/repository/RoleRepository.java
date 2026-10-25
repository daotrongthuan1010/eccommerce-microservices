package com.dtthuan3.ecommerce.authservice.iam.repository;

import com.dtthuan3.ecommerce.authservice.iam.entity.Role;
import com.dtthuan3.ecommerce.authservice.iam.entity.Tenant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByCodeAndTenant(String code, Tenant tenant);

    List<Role> findByTenantId(Long tenantId);

    List<Role> findByTenantIsNull();

    boolean existsByCodeAndTenant(String code, Tenant tenant);
}
