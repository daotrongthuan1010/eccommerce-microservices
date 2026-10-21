
package com.dtthuan3.ecommerce.productservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AttributeOptionUpdateRequest(

        @NotBlank(message = "Giá trị lựa chọn không được để trống")
        @Size(max = 150, message = "Giá trị lựa chọn tối đa 150 ký tự")
        String value,

        Integer displayOrder
) {
}
