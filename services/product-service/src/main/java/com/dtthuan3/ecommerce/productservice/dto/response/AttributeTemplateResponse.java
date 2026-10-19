
package com.dtthuan3.ecommerce.productservice.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record AttributeTemplateResponse(
        UUID id,
        String name,
        String code,
        String description,
        Boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<AttributeTemplateItemResponse> items
) {
}
