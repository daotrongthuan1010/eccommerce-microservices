package com.dtthuan3.ecommerce.productservice.mapper;

import com.dtthuan3.ecommerce.productservice.domain.SkuMedia;
import com.dtthuan3.ecommerce.productservice.domain.VariantAttributeValue;
import com.dtthuan3.ecommerce.productservice.dto.request.SkuMediaRequest;
import com.dtthuan3.ecommerce.productservice.dto.request.VariantAttributeValueRequest;
import com.dtthuan3.ecommerce.productservice.dto.response.VariantAttributeValueResponse;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface VariantAttributeValueMapper {

    VariantAttributeValue toEntity(
            VariantAttributeValueRequest request
    );

    VariantAttributeValueResponse toResponse(
            VariantAttributeValue entity
    );

    void updateEntity(
            VariantAttributeValueRequest request,
            @MappingTarget VariantAttributeValue entity
    );
}
