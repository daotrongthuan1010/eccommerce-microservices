package com.dtthuan3.ecommerce.productservice.repository;

import com.dtthuan3.ecommerce.productservice.domain.VariantAttributeValue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VariantAttributeValueRepository
        extends JpaRepository<VariantAttributeValue, Long> {

    List<VariantAttributeValue> findByVariantId(Long variantId);

    Optional<VariantAttributeValue> findByVariantIdAndAttributeId(
            Long variantId,
            Long attributeId
    );

    boolean existsByVariantIdAndAttributeId(
            Long variantId,
            Long attributeId
    );

    boolean existsByVariantIdAndAttributeIdAndIdNot(
            Long variantId,
            Long attributeId,
            Long id
    );

    void deleteByVariantId(Long variantId);
}