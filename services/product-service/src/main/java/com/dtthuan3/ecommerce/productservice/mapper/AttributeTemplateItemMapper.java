package com.dtthuan3.ecommerce.productservice.mapper;

import com.dtthuan3.ecommerce.productservice.domain.AttributeOption;
import com.dtthuan3.ecommerce.productservice.domain.AttributeTemplateItem;
import com.dtthuan3.ecommerce.productservice.dto.request.AttributeOptionRequest;
import com.dtthuan3.ecommerce.productservice.dto.request.AttributeTemplateItemRequest;
import com.dtthuan3.ecommerce.productservice.dto.response.AttributeTemplateItemResponse;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface AttributeTemplateItemMapper {

    AttributeTemplateItem toEntity(
            AttributeTemplateItemRequest request
    );

    AttributeTemplateItemResponse toResponse(
            AttributeTemplateItem entity
    );

    void updateEntity(
            AttributeTemplateItemRequest request,
            @MappingTarget AttributeTemplateItem entity
    );
}
