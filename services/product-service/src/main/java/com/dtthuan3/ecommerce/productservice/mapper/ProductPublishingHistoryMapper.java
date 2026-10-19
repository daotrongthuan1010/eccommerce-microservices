
package com.dtthuan3.ecommerce.productservice.mapper;

import com.dtthuan3.ecommerce.productservice.domain.ProductPublishingHistory;
import com.dtthuan3.ecommerce.productservice.dto.request.ProductPublishingHistoryRequest;
import com.dtthuan3.ecommerce.productservice.dto.response.ProductPublishingHistoryResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ProductPublishingHistoryMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "revisionId", ignore = true)
    @Mapping(target = "changedAt", ignore = true)
    ProductPublishingHistory toEntity(
            ProductPublishingHistoryRequest request
    );

    ProductPublishingHistoryResponse toResponse(
            ProductPublishingHistory entity
    );

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "revisionId", ignore = true)
    @Mapping(target = "changedAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(
            ProductPublishingHistoryRequest request,
            @MappingTarget ProductPublishingHistory entity
    );
}
