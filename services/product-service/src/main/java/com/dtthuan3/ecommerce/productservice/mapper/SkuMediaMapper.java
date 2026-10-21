package com.dtthuan3.ecommerce.productservice.mapper;

import com.dtthuan3.ecommerce.productservice.domain.SkuMedia;
import com.dtthuan3.ecommerce.productservice.dto.response.SkuMediaResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SkuMediaMapper {

    @Mapping(source = "fileKey", target = "storageKey")
    @Mapping(target = "fileName", ignore = true)
    @Mapping(target = "contentType", ignore = true)
    SkuMediaResponse toResponse(SkuMedia entity);
}