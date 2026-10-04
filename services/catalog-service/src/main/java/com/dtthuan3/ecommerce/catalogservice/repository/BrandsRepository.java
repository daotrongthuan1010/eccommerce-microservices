package com.dtthuan3.ecommerce.catalogservice.repository;

import com.dtthuan3.ecommerce.catalogservice.contstant.BrandsStatus;
import com.dtthuan3.ecommerce.catalogservice.entity.Brands;
import org.springframework.data.jpa.repository.JpaRepository;

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

}