package com.dtthuan3.ecommerce.productservice.mapper;

import com.dtthuan3.ecommerce.productservice.domain.Attribute;
import com.dtthuan3.ecommerce.productservice.domain.AttributeOption;
import com.dtthuan3.ecommerce.productservice.dto.request.AttributeOptionRequest;
import com.dtthuan3.ecommerce.productservice.dto.request.AttributeRequest;
import com.dtthuan3.ecommerce.productservice.dto.response.AttributeOptionResponse;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface AttributeOptionMapper {

    AttributeOption toEntity(AttributeOptionRequest request);

    AttributeOptionResponse toResponse(AttributeOption entity);

    void updateEntity(
            AttributeOptionRequest request,
            @MappingTarget AttributeOption entity
    );
}
