package com.dtthuan3.ecommerce.paymentservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

/** Payload IPN/callback tu fake-gateway. Canonical ky: provider.txnRef.amount.resultCode.timestamp */
public record ProviderCallbackRequest(
        @NotBlank String txnRef,
        @PositiveOrZero long amount,
        @NotBlank String resultCode,
        String status,
        long timestamp,
        @NotBlank String signature) {

    public boolean isSuccess() {
        return "00".equals(resultCode) || "SUCCESS".equalsIgnoreCase(String.valueOf(status));
    }
}
