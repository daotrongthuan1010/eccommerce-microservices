package com.dtthuan3.ecommerce.productservice.repository;

import com.dtthuan3.ecommerce.productservice.domain.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductVariantRepository
        extends JpaRepository<ProductVariant, UUID> {

    boolean existsByProductIdAndVariantCode(
            UUID productId,
            String variantCode
    );

    Optional<ProductVariant> findByIdAndProductId(
            UUID id,
            UUID productId
    );

    List<ProductVariant> findAllByProductIdOrderByCreatedAtDesc(
            UUID productId
    );
}