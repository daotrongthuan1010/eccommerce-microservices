package com.dtthuan3.ecommerce.productservice.dto.request;

import com.dtthuan3.ecommerce.productservice.constant.AttributeDataType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AttributeCreateRequest(

        @NotBlank(message = "Tên thuộc tính không được để trống")
        @Size(max = 150, message = "Tên thuộc tính tối đa 150 ký tự")
        String name,

        @NotBlank(message = "Mã thuộc tính không được để trống")
        @Size(max = 100, message = "Mã thuộc tính tối đa 100 ký tự")
        @Pattern(
                regexp = "^[a-zA-Z0-9_-]+$",
                message = "Mã thuộc tính chỉ được chứa chữ, số, dấu gạch ngang và gạch dưới"
        )
        String code,

        @NotNull(message = "Kiểu dữ liệu không được để trống")
        AttributeDataType dataType,

        @Size(max = 50, message = "Đơn vị đo tối đa 50 ký tự")
        String unit,

        Boolean required
) {
}
