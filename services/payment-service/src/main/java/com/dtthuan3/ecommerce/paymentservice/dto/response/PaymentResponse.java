package com.dtthuan3.ecommerce.paymentservice.dto.response;

import com.dtthuan3.ecommerce.paymentservice.domain.PaymentMethod;
import com.dtthuan3.ecommerce.paymentservice.domain.PaymentStatus;
import com.dtthuan3.ecommerce.paymentservice.entity.Payment;
import java.time.Instant;

public record PaymentResponse(
        String id,
        String orderId,
        PaymentMethod method,
        long amount,
        String currency,
        PaymentStatus status,
        String checkoutUrl,
        String qrContent,
        Instant expiresAt,
        Instant paidAt,
        long refundedAmount,
        String failureCode,
        String failureMessage,
        boolean latePayment,
        Instant createdAt,
        Instant updatedAt) {

    public static PaymentResponse from(Payment p) {
        return new PaymentResponse(p.getId(), p.getOrderId(), p.getMethod(), p.getAmount(), p.getCurrency(),
                p.getStatus(), p.getCheckoutUrl(), p.getQrContent(), p.getExpiresAt(), p.getPaidAt(),
                p.getRefundedAmount(), p.getFailureCode(), p.getFailureMessage(), p.isLatePayment(),
                p.getCreatedAt(), p.getUpdatedAt());
    }
}
