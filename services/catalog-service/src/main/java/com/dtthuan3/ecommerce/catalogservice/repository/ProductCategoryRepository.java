package com.dtthuan3.ecommerce.catalogservice.repository;

import com.dtthuan3.ecommerce.catalogservice.entity.ProductCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductCategoryRepository
        extends JpaRepository<ProductCategory, String> {

    List<ProductCategory> findByProductId(UUID productId);

    List<ProductCategory> findByCategoryId(String categoryId);

    Optional<ProductCategory> findByProductIdAndCategoryId(
            UUID productId,
            String categoryId
    );

    boolean existsByProductIdAndCategoryId(
            UUID productId,
            String categoryId
    );

    Optional<ProductCategory> findByProductIdAndPrimaryTrue(
            UUID productId
    );

    boolean existsByProductIdAndPrimaryTrue(
            UUID productId
    );

    void deleteByProductIdAndCategoryId(
            UUID productId,
            String categoryId
    );}