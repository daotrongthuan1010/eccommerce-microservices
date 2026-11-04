package com.dtthuan3.ecommerce.paymentservice.dto.request;

import com.dtthuan3.ecommerce.paymentservice.domain.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreatePaymentRequest(
        @NotBlank @Size(max = 64) String orderId,
        @NotNull PaymentMethod method,
        @Positive long amount,
        @Size(max = 3) String currency,
        @Size(max = 1024) String returnUrl,
        @Size(max = 512) String description) {

    public String currencyOrDefault() {
        return currency == null || currency.isBlank() ? "VND" : currency.toUpperCase();
    }
}
