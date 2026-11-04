package com.dtthuan3.ecommerce.orderservice.dto.response;

import java.time.Instant;
import java.util.List;

/** Ket qua checkout: tra ve don + payment neu online (checkoutUrl/qr de FE sang buoc thanh toan). */
public record CheckoutResponse(
        String orderId,
        String orderCode,
        String status,
        long totalAmount,
        String currency,
        String paymentMethod,
        String paymentId,
        String paymentStatus,
        String checkoutUrl,
        String qrContent,
        Instant expiresAt,
        List<Line> lines) {

    public record Line(
            String skuId,
            String productName,
            long unitPrice,
            int quantity,
            long lineAmount) {
    }
}
