
package com.dtthuan3.ecommerce.productservice.repository;

import com.dtthuan3.ecommerce.productservice.domain.AttributeOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AttributeOptionRepository
        extends JpaRepository<AttributeOption, UUID> {

    List<AttributeOption>
    findByAttributeIdAndActiveTrueOrderByDisplayOrderAscValueAsc(
            UUID attributeId
    );

    Optional<AttributeOption> findByIdAndAttributeId(
            UUID id,
            UUID attributeId
    );

    boolean existsByAttributeIdAndValueIgnoreCase(
            UUID attributeId,
            String value
    );

    boolean existsByAttributeIdAndValueIgnoreCaseAndIdNot(
            UUID attributeId,
            String value,
            UUID id
    );
}
