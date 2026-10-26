package com.dtthuan3.ecommerce.inventoryservice.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Reservation header và tiến độ từng dòng; quantity còn giữ được tính từ requested-dispatched-released. */
public record InventoryReservationResponse(String reservationId, String orderId, String enterpriseId,
                                           String status, Instant expiresAt, long version,
                                           String createdBy, Instant createdAt, String committedBy,
                                           Instant committedAt, List<Line> lines) {
    /** Tiến độ từng SKU đã giữ, dispatch và release. */
    public record Line(String reservationLineId, int lineNumber, String inventoryId, String sku,
                       BigDecimal requestedQuantity, BigDecimal dispatchedQuantity,
                       BigDecimal releasedQuantity, BigDecimal remainingQuantity) {}
}
