
package com.dtthuan3.ecommerce.productservice.dto.response;

import java.util.UUID;

public record AttributeTemplateItemResponse(
        UUID id,
        UUID templateId,
        UUID attributeId,
        Boolean variantAttribute,
        Integer displayOrder,
        Boolean required,
        Boolean active
) {
}
