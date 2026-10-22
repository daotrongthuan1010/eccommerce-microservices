
package com.dtthuan3.ecommerce.productservice.mapper;

import com.dtthuan3.ecommerce.productservice.domain.Attribute;
import com.dtthuan3.ecommerce.productservice.dto.response.AttributeResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AttributeMapper {

    AttributeResponse toResponse(Attribute attribute);
}
