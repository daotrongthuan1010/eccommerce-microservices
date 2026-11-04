package com.dtthuan3.ecommerce.paymentservice.dto.response;

import com.dtthuan3.ecommerce.paymentservice.domain.RefundStatus;
import com.dtthuan3.ecommerce.paymentservice.entity.PaymentRefund;
import java.time.Instant;

public record RefundResponse(
        String id, String paymentId, long amount, String reason, RefundStatus status, Instant createdAt) {

    public static RefundResponse from(PaymentRefund r) {
        return new RefundResponse(r.getId(), r.getPaymentId(), r.getAmount(), r.getReason(), r.getStatus(), r.getCreatedAt());
    }
}
