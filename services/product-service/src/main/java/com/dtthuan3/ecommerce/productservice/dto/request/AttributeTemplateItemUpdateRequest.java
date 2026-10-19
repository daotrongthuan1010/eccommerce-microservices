
package com.dtthuan3.ecommerce.productservice.dto.request;

import jakarta.validation.constraints.PositiveOrZero;

public record AttributeTemplateItemUpdateRequest(

        Boolean variantAttribute,

        @PositiveOrZero(message = "Thứ tự hiển thị phải lớn hơn hoặc bằng 0")
        Integer displayOrder,

        Boolean required
) {
}
