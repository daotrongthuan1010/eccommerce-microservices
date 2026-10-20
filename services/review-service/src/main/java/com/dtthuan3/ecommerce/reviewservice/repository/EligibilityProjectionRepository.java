package com.dtthuan3.ecommerce.reviewservice.repository;

import com.dtthuan3.ecommerce.reviewservice.entity.EligibilityProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EligibilityProjectionRepository extends JpaRepository<EligibilityProjection, String> {
    Optional<EligibilityProjection> findByLineId(String lineId);
}
