package com.dtthuan3.ecommerce.shippingservice.dto.request;

import jakarta.validation.constraints.NotBlank;

/** Webhook trang thai tu fake carrier. Canonical ky: carrierCode.status.timestamp */
public record CarrierWebhookRequest(
        @NotBlank String carrierCode,
        @NotBlank String status,
        long timestamp,
        @NotBlank String signature,
        String note) {}
