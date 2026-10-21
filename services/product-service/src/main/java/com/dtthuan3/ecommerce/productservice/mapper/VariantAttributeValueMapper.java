package com.dtthuan3.ecommerce.productservice.mapper;

import com.dtthuan3.ecommerce.productservice.domain.VariantAttributeValue;
import com.dtthuan3.ecommerce.productservice.dto.request.VariantAttributeValueRequest;
import com.dtthuan3.ecommerce.productservice.dto.response.VariantAttributeValueResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface VariantAttributeValueMapper {

    @Mapping(target = "id", ignore = true)
    VariantAttributeValue toEntity(
            VariantAttributeValueRequest request
    );

    VariantAttributeValueResponse toResponse(
            VariantAttributeValue entity
    );

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(
            VariantAttributeValueRequest request,
            @MappingTarget VariantAttributeValue entity
    );
}