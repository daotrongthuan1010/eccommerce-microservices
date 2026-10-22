
package com.dtthuan3.ecommerce.productservice.repository;

import com.dtthuan3.ecommerce.productservice.domain.AttributeTemplateItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AttributeTemplateItemRepository
        extends JpaRepository<AttributeTemplateItem, UUID> {

    List<AttributeTemplateItem>
    findAllByTemplateIdAndActiveTrueOrderByDisplayOrderAsc(UUID templateId);

    Optional<AttributeTemplateItem> findByIdAndTemplateId(
            UUID id,
            UUID templateId
    );

    Optional<AttributeTemplateItem> findByTemplateIdAndAttributeId(
            UUID templateId,
            UUID attributeId
    );


    boolean existsByTemplateIdAndAttributeId(
            UUID templateId,
            UUID attributeId
    );

    boolean existsByTemplateIdAndAttributeIdAndIdNot(
            UUID templateId,
            UUID attributeId,
            UUID id
    );

    boolean existsByAttributeId(UUID attributeId);
}
