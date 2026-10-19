package com.dtthuan3.ecommerce.productservice.mapper;

import com.dtthuan3.ecommerce.productservice.domain.ProductMedia;
import com.dtthuan3.ecommerce.productservice.dto.response.ProductMediaResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProductMediaMapper {

    @Mapping(source = "fileKey", target = "storageKey")
    @Mapping(target = "fileName", ignore = true)
    @Mapping(target = "contentType", ignore = true)
    ProductMediaResponse toResponse(ProductMedia entity);
}