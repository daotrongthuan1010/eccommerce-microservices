package com.dtthuan3.ecommerce.productservice.mapper;

import com.dtthuan3.ecommerce.productservice.domain.Sku;
import com.dtthuan3.ecommerce.productservice.dto.request.SkuRequest;
import com.dtthuan3.ecommerce.productservice.dto.response.SkuResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface SkuMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "sellingStatus", ignore = true)
    Sku toEntity(SkuRequest request);

    @Mapping(target = "active", ignore = true)
    SkuResponse toResponse(Sku entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "sellingStatus", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(
            SkuRequest request,
            @MappingTarget Sku entity
    );
}