package com.dtthuan3.ecommerce.authservice.iam.repository;

import com.dtthuan3.ecommerce.authservice.iam.entity.Position;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PositionRepository extends JpaRepository<Position, Long> {

    List<Position> findByTenantId(Long tenantId);
}
