
package com.dtthuan3.ecommerce.productservice.mapper;

import com.dtthuan3.ecommerce.productservice.domain.AttributeOption;
import com.dtthuan3.ecommerce.productservice.dto.response.AttributeOptionResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AttributeOptionMapper {

    AttributeOptionResponse toResponse(AttributeOption option);
}
