package com.dtthuan3.ecommerce.productservice.repository;

import com.dtthuan3.ecommerce.productservice.domain.AttributeTemplateItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AttributeTemplateItemRepository
        extends JpaRepository<AttributeTemplateItem, Long> {

    List<AttributeTemplateItem> findByTemplateId(Long templateId);

    List<AttributeTemplateItem> findByTemplateIdAndActiveTrue(
            Long templateId
    );

    Optional<AttributeTemplateItem> findByTemplateIdAndAttributeId(
            Long templateId,
            Long attributeId
    );

    boolean existsByTemplateIdAndAttributeId(
            Long templateId,
            Long attributeId
    );

    boolean existsByTemplateIdAndAttributeIdAndIdNot(
            Long templateId,
            Long attributeId,
            Long id
    );

    void deleteByTemplateId(Long templateId);
}