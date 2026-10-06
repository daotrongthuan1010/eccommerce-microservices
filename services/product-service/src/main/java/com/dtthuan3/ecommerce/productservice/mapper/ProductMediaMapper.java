package com.dtthuan3.ecommerce.productservice.mapper;

import com.dtthuan3.ecommerce.productservice.domain.ProductMedia;
import com.dtthuan3.ecommerce.productservice.dto.request.ProductMediaRequest;
import com.dtthuan3.ecommerce.productservice.dto.response.ProductMediaResponse;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ProductMediaMapper {

    ProductMedia toEntity(ProductMediaRequest request);

    ProductMediaResponse toResponse(ProductMedia entity);

    void updateEntity(
            ProductMediaRequest request,
            @MappingTarget ProductMedia entity
    );
}
