package com.dtthuan3.ecommerce.orderservice.dto.response;

import java.time.Instant;
import java.util.List;

/** Don hang cua toi (GET /me/orders): nguoi dung chi thay don cua minh. */
public record OrderResponse(
        String id,
        String orderCode,
        String status,
        long totalAmount,
        String currency,
        String paymentMethod,
        String paymentId,
        String shippingAddress,
        String recipientName,
        String recipientPhone,
        String cancelReason,
        Instant createdAt,
        Instant updatedAt,
        List<CheckoutResponse.Line> lines) {
}
