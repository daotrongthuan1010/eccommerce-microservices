package com.dtthuan3.ecommerce.productservice.mapper;

import com.dtthuan3.ecommerce.productservice.domain.AttributeTemplateItem;
import com.dtthuan3.ecommerce.productservice.dto.response.AttributeTemplateItemResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.UUID;

@Mapper(componentModel = "spring")
public interface AttributeTemplateItemMapper {

    AttributeTemplateItemResponse toResponse(
            AttributeTemplateItem entity
    );

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "templateId", ignore = true)
    @Mapping(target = "active", constant = "true")
    AttributeTemplateItem toNewEntity(
            UUID attributeId,
            Boolean variantAttribute,
            Integer displayOrder,
            Boolean required
    );
}