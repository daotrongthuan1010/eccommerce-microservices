package com.dtthuan3.ecommerce.productservice.mapper;

import com.dtthuan3.ecommerce.productservice.domain.AttributeTemplate;
import com.dtthuan3.ecommerce.productservice.domain.ProductAttributeValue;
import com.dtthuan3.ecommerce.productservice.dto.request.AttributeTemplateRequest;
import com.dtthuan3.ecommerce.productservice.dto.request.ProductAttributeValueRequest;
import com.dtthuan3.ecommerce.productservice.dto.response.ProductAttributeValueResponse;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ProductAttributeValueMapper {

    ProductAttributeValue toEntity(
            ProductAttributeValueRequest request
    );

    ProductAttributeValueResponse toResponse(
            ProductAttributeValue entity
    );

    void updateEntity(
            ProductAttributeValueRequest request,
            @MappingTarget ProductAttributeValue entity
    );
}
