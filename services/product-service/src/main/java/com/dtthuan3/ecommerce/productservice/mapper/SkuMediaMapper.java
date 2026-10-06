package com.dtthuan3.ecommerce.productservice.mapper;

import com.dtthuan3.ecommerce.productservice.domain.ProductPublishingHistory;
import com.dtthuan3.ecommerce.productservice.domain.SkuMedia;
import com.dtthuan3.ecommerce.productservice.dto.request.ProductPublishingHistoryRequest;
import com.dtthuan3.ecommerce.productservice.dto.request.SkuMediaRequest;
import com.dtthuan3.ecommerce.productservice.dto.response.SkuMediaResponse;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface SkuMediaMapper {

    SkuMedia toEntity(SkuMediaRequest request);

    SkuMediaResponse toResponse(SkuMedia entity);

    void updateEntity(
            SkuMediaRequest request,
            @MappingTarget SkuMedia entity
    );
}
