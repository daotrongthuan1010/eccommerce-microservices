package com.dtthuan3.ecommerce.productservice.mapper;

import com.dtthuan3.ecommerce.productservice.domain.AttributeTemplate;
import com.dtthuan3.ecommerce.productservice.dto.response.AttributeTemplateResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AttributeTemplateMapper {

    @Mapping(target = "items", ignore = true)
    AttributeTemplateResponse toResponse(AttributeTemplate entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "active", constant = "true")
    AttributeTemplate toNewEntity(
            String name,
            String code,
            String description
    );
}