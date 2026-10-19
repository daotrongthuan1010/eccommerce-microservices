package com.dtthuan3.ecommerce.productservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateProductVariantRequest(

        @NotBlank(message = "Mã biến thể không được để trống")
        @Size(max = 100, message = "Mã biến thể tối đa 100 ký tự")
        String variantCode,

        @NotBlank(message = "Tên biến thể không được để trống")
        @Size(max = 255, message = "Tên biến thể tối đa 255 ký tự")
        String name,

        String description
) {
}