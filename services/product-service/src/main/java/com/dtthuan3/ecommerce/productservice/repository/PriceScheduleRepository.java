
package com.dtthuan3.ecommerce.productservice.repository;

import com.dtthuan3.ecommerce.productservice.domain.PriceSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface PriceScheduleRepository
        extends JpaRepository<PriceSchedule, UUID> {

    List<PriceSchedule> findBySkuIdOrderByEffectiveFromDesc(
            UUID skuId
    );

    /**
     * Marketplace browse: price-schedule ACTIVE con hieu luc cho nhieu SKU
     * (IN query). ACTIVE + effectiveFrom <= now AND (effectiveTo IS NULL OR
     * effectiveTo > now).
     */
    @Query("""
        SELECT ps FROM PriceSchedule ps
        WHERE ps.skuId IN :skuIds
          AND ps.status = 'ACTIVE'
          AND ps.effectiveFrom <= :now
          AND (ps.effectiveTo IS NULL OR ps.effectiveTo > :now)
        """)
    List<PriceSchedule> findActiveForSkus(
            @Param("skuIds") java.util.Collection<UUID> skuIds,
            @Param("now") LocalDateTime now
    );

    @Query("""
        SELECT COUNT(p) > 0
        FROM PriceSchedule p
        WHERE p.skuId = :skuId
          AND p.status <> 'CANCELLED'
          AND (:excludeId IS NULL OR p.id <> :excludeId)
          AND (:newEffectiveTo IS NULL OR p.effectiveFrom < :newEffectiveTo)
          AND (p.effectiveTo IS NULL OR p.effectiveTo > :newEffectiveFrom)
        """)
    boolean existsOverlappingSchedule(
            @Param("skuId") UUID skuId,
            @Param("newEffectiveFrom") LocalDateTime newEffectiveFrom,
            @Param("newEffectiveTo") LocalDateTime newEffectiveTo,
            @Param("excludeId") UUID excludeId
    );
}
