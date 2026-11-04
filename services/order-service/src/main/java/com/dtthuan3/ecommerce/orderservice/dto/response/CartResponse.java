package com.dtthuan3.ecommerce.orderservice.dto.response;

import java.time.Instant;
import java.util.List;

public record CartResponse(
        String id,
        String userId,
        List<Item> items,
        int totalQuantity,
        long totalAmount,
        Instant updatedAt) {

    public record Item(
            String skuId,
            String productName,
            long unitPrice,
            int quantity,
            long lineAmount) {
    }
}
