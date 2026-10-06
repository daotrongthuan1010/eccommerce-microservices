package com.dtthuan3.ecommerce.productservice.mapper;

import com.dtthuan3.ecommerce.productservice.domain.ProductVariant;
import com.dtthuan3.ecommerce.productservice.dto.request.ProductVariantRequest;
import com.dtthuan3.ecommerce.productservice.dto.response.ProductVariantResponse;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ProductVariantMapper {

    ProductVariant toEntity(
            ProductVariantRequest request
    );

    ProductVariantResponse toResponse(
            ProductVariant entity
    );

    void updateEntity(
            ProductVariantRequest request,
            @MappingTarget ProductVariant entity
    );
}
