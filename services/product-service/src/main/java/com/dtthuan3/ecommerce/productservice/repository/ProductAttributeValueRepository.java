
package com.dtthuan3.ecommerce.productservice.repository;

import com.dtthuan3.ecommerce.productservice.domain.ProductAttributeValue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProductAttributeValueRepository
        extends JpaRepository<ProductAttributeValue, UUID> {

    List<ProductAttributeValue> findAllByRevisionId(UUID revisionId);

    boolean existsByRevisionIdAndAttributeId(
            UUID revisionId,
            UUID attributeId
    );

    boolean existsByAttributeId(UUID attributeId);

    boolean existsByOptionId(UUID optionId);

    void deleteAllByRevisionId(UUID revisionId);
}
