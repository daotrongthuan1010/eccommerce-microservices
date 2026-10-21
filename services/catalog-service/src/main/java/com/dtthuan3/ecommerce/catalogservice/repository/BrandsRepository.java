package com.dtthuan3.ecommerce.catalogservice.repository;

import com.dtthuan3.ecommerce.catalogservice.contstant.BrandsStatus;
import com.dtthuan3.ecommerce.catalogservice.entity.Brands;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BrandsRepository extends JpaRepository<Brands, String> {

    Optional<Brands> findBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, String id);

    Page<Brands> findByStatus(
            BrandsStatus status,
            Pageable pageable
    );

    Page<Brands> findByNameContainingIgnoreCase(
            String name,
            Pageable pageable
    );

    List<Brands> findByStatusOrderByNameAsc(
            BrandsStatus status
    );
}