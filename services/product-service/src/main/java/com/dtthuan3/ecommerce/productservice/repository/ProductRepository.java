package com.dtthuan3.ecommerce.productservice.repository;

import com.dtthuan3.ecommerce.productservice.domain.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ProductRepository
        extends JpaRepository<Product, UUID>, JpaSpecificationExecutor<Product> {

    boolean existsByProductCode(String productCode);

    Optional<Product> findByProductCode(String productCode);

    /**
     * Marketplace browse (2026-11-05 fe-marketplace D4): chi san pham da xuat ban
     * (status ACTIVE + co publishedRevisionId). Keyword LIKE ten trong published
     * revision — khong join trong page query de giu index don bang, enrich ten
     * o lop service.
     */
    @Query("""
        SELECT p FROM Product p
        WHERE p.status = 'ACTIVE'
          AND p.publishedRevisionId IS NOT NULL
          AND (:categoryId IS NULL OR p.categoryId = :categoryId)
          AND (:brandId IS NULL OR p.brandId = :brandId)
        ORDER BY p.updatedAt DESC
        """)
    Page<Product> findPublishedForBrowse(
            @Param("categoryId") UUID categoryId,
            @Param("brandId") UUID brandId,
            Pageable pageable
    );
}