package com.dtthuan3.ecommerce.shippingservice.dto.request;

import com.dtthuan3.ecommerce.shippingservice.domain.ServiceLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record CreateQuoteRequest(
        @NotBlank String fromProvinceCode,
        @NotBlank String toProvinceCode,
        @Positive int weightGrams,
        Integer lengthCm,
        Integer widthCm,
        Integer heightCm,
        ServiceLevel serviceLevel) {

    public ServiceLevel levelOrDefault() {
        return serviceLevel == null ? ServiceLevel.STANDARD : serviceLevel;
    }
}
