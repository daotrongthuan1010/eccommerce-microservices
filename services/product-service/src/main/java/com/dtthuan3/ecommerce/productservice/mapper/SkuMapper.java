package com.dtthuan3.ecommerce.productservice.mapper;

import com.dtthuan3.ecommerce.productservice.domain.Sku;
import com.dtthuan3.ecommerce.productservice.dto.request.SkuRequest;
import com.dtthuan3.ecommerce.productservice.dto.response.SkuResponse;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface SkuMapper {

    Sku toEntity(SkuRequest request);

    SkuResponse toResponse(Sku entity);

    void updateEntity(
            SkuRequest request,
            @MappingTarget Sku entity
    );
}
