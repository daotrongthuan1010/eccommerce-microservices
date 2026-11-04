package com.dtthuan3.ecommerce.paymentservice.dto.request;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateRefundRequest(
        @Positive long amount,
        @Size(max = 512) String reason) {}
