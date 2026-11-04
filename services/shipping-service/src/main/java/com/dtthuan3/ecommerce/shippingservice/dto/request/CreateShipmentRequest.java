package com.dtthuan3.ecommerce.shippingservice.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record CreateShipmentRequest(
        @NotBlank String orderId,
        @NotBlank String quoteId,
        @PositiveOrZero long expectedFee,
        @Valid @NotNull Address sender,
        @Valid @NotNull Address recipient,
        @PositiveOrZero long codAmount,
        String note) {

    public record Address(
            @NotBlank String name,
            @NotBlank String phone,
            @NotBlank String address,
            @NotBlank String provinceCode) {}
}
