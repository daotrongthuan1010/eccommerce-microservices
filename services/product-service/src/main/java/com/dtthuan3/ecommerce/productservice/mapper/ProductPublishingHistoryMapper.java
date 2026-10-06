package com.dtthuan3.ecommerce.productservice.mapper;

import com.dtthuan3.ecommerce.productservice.domain.ProductAttributeValue;
import com.dtthuan3.ecommerce.productservice.domain.ProductPublishingHistory;
import com.dtthuan3.ecommerce.productservice.dto.request.ProductAttributeValueRequest;
import com.dtthuan3.ecommerce.productservice.dto.request.ProductPublishingHistoryRequest;
import com.dtthuan3.ecommerce.productservice.dto.response.ProductPublishingHistoryResponse;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ProductPublishingHistoryMapper {

    ProductPublishingHistory toEntity(
            ProductPublishingHistoryRequest request
    );

    ProductPublishingHistoryResponse toResponse(
            ProductPublishingHistory entity
    );

    void updateEntity(
            ProductPublishingHistoryRequest request,
            @MappingTarget ProductPublishingHistory entity
    );
}
