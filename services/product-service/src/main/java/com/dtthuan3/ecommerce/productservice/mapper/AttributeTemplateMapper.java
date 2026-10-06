package com.dtthuan3.ecommerce.productservice.mapper;

import com.dtthuan3.ecommerce.productservice.domain.AttributeTemplate;
import com.dtthuan3.ecommerce.productservice.dto.request.AttributeTemplateRequest;
import com.dtthuan3.ecommerce.productservice.dto.response.AttributeTemplateResponse;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface AttributeTemplateMapper {

    AttributeTemplate toEntity(AttributeTemplateRequest request);

    AttributeTemplateResponse toResponse(AttributeTemplate entity);

    void updateEntity(
            AttributeTemplateRequest request,
            @MappingTarget AttributeTemplate entity
    );
}
