package com.dtthuan3.ecommerce.productservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record SkuValidationRequest(
        @NotEmpty
        @Size(max = 500)
        List<@NotBlank String> skuCodes
) {
}
