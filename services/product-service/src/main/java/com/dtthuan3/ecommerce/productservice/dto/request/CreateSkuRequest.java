package com.dtthuan3.ecommerce.productservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateSkuRequest(

        @NotBlank(message = "Mã SKU không được để trống")
        @Size(max = 100, message = "Mã SKU tối đa 100 ký tự")
        String skuCode,

        @NotBlank(message = "Barcode không được để trống")
        @Size(max = 100, message = "Barcode tối đa 100 ký tự")
        String barcode
) {
}