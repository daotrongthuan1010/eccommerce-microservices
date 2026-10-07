package com.dtthuan3.ecommerce.catalogservice.repository;

import com.dtthuan3.ecommerce.catalogservice.contstant.BrandsStatus;
import com.dtthuan3.ecommerce.catalogservice.entity.Brands;
import io.lettuce.core.dynamic.annotation.Param;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;


import java.util.List;
import java.util.Optional;

public interface BrandsRepository
        extends JpaRepository<Brands, Long> {

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(
            String slug,
            Long id
    );

    Optional<Brands> findBySlug(String slug);

    List<Brands> findAllByStatusOrderByNameAsc(
            BrandsStatus status
    );

    @Query("""
        SELECT b
        FROM Brands b
        WHERE b.status = :status
          AND (
              LOWER(b.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
              OR LOWER(b.slug) LIKE LOWER(CONCAT('%', :keyword, '%'))
          )
    """)
    Page<Brands> search(
            @Param("keyword") String keyword,
            @Param("status") BrandsStatus status,
            Pageable pageable
    );
}