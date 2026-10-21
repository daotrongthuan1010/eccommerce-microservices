
package com.dtthuan3.ecommerce.productservice.repository;

import com.dtthuan3.ecommerce.productservice.domain.VariantAttributeValue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VariantAttributeValueRepository
        extends JpaRepository<VariantAttributeValue, UUID> {

    List<VariantAttributeValue> findAllByVariantId(UUID variantId);

    Optional<VariantAttributeValue> findByVariantIdAndAttributeId(
            UUID variantId,
            UUID attributeId
    );

    boolean existsByVariantIdAndAttributeId(
            UUID variantId,
            UUID attributeId
    );

    boolean existsByAttributeId(UUID attributeId);

    boolean existsByOptionId(UUID optionId);

    boolean existsByVariantIdAndAttributeIdAndIdNot(
            UUID variantId,
            UUID attributeId,
            UUID id
    );

    void deleteAllByVariantId(UUID variantId);
}
