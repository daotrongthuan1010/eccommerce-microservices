package com.dtthuan3.ecommerce.productservice.mapper;

import com.dtthuan3.ecommerce.productservice.domain.ProductAttributeValue;
import com.dtthuan3.ecommerce.productservice.dto.request.ProductAttributeValueRequest;
import com.dtthuan3.ecommerce.productservice.dto.response.ProductAttributeValueResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ProductAttributeValueMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "revisionId", ignore = true)
    ProductAttributeValue toEntity(
            ProductAttributeValueRequest request
    );

    @Mapping(target = "productId", ignore = true)
    ProductAttributeValueResponse toResponse(
            ProductAttributeValue entity
    );

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "revisionId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(
            ProductAttributeValueRequest request,
            @MappingTarget ProductAttributeValue entity
    );
}