
package com.dtthuan3.ecommerce.productservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AttributeTemplateCreateRequest(

        @NotBlank(message = "Tên template không được để trống")
        @Size(max = 150, message = "Tên template không được vượt quá 150 ký tự")
        String name,

        @NotBlank(message = "Mã template không được để trống")
        @Size(max = 100, message = "Mã template không được vượt quá 100 ký tự")
        String code,

        @Size(max = 500, message = "Mô tả không được vượt quá 500 ký tự")
        String description
) {
}
