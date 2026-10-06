package com.dtthuan3.ecommerce.productservice.repository;

import com.dtthuan3.ecommerce.productservice.domain.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductVariantRepository
        extends JpaRepository<ProductVariant, Long> {

    List<ProductVariant> findByProductId(Long productId);

    List<ProductVariant> findByProductIdAndActiveTrue(
            Long productId
    );

    Optional<ProductVariant> findByVariantCode(String variantCode);

    boolean existsByVariantCode(String variantCode);

    boolean existsByVariantCodeAndIdNot(
            String variantCode,
            Long id
    );
}