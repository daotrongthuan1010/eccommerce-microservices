
package com.dtthuan3.ecommerce.productservice.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.UUID;

public record AttributeTemplateItemCreateRequest(

        @NotNull(message = "ID thuộc tính không được để trống")
        UUID attributeId,

        Boolean variantAttribute,

        @PositiveOrZero(message = "Thứ tự hiển thị phải lớn hơn hoặc bằng 0")
        Integer displayOrder,

        Boolean required
) {
}
