
package com.dtthuan3.ecommerce.productservice.dto.response;

import com.dtthuan3.ecommerce.productservice.constant.AttributeDataType;

import java.util.UUID;

public record AttributeResponse(
        UUID id,
        String name,
        String code,
        AttributeDataType dataType,
        String unit,
        Boolean required,
        Boolean active
) {
}
