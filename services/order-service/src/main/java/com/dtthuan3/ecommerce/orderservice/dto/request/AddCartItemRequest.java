package com.dtthuan3.ecommerce.orderservice.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Them 1 SKU vao gio hoac tang so luong dong co san. */
public record AddCartItemRequest(
        @NotBlank @Size(max = 64) String skuId,
        @NotBlank @Size(max = 255) String productName,
        @NotNull @Min(0) long unitPrice,
        @NotNull @Min(1) @Max(999) int quantity) {
}
