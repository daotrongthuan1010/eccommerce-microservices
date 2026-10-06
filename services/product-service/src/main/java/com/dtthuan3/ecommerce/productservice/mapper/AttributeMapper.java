package com.dtthuan3.ecommerce.productservice.mapper;

import com.dtthuan3.ecommerce.productservice.domain.Attribute;
import com.dtthuan3.ecommerce.productservice.dto.request.AttributeRequest;
import com.dtthuan3.ecommerce.productservice.dto.response.AttributeResponse;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface AttributeMapper {

    Attribute toEntity(AttributeRequest request);

    AttributeResponse toResponse(Attribute entity);

    void updateEntity(
            AttributeRequest request,
            @MappingTarget Attribute entity
    );
}
