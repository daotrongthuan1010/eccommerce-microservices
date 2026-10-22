
package com.dtthuan3.ecommerce.productservice.dto.response;

import java.util.UUID;

public record AttributeOptionResponse(
        UUID id,
        UUID attributeId,
        String value,
        Integer displayOrder,
        Boolean active
) {
}
