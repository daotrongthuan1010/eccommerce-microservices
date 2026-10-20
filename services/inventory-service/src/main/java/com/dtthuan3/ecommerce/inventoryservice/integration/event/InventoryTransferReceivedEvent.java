package com.dtthuan3.ecommerce.inventoryservice.integration.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Sự kiện outbox phát sau khi tồn hai đầu và kết quả nhận điều chuyển đã commit. */
public record InventoryTransferReceivedEvent(
        String eventId,
        String eventType,
        Instant occurredAt,
        String transferId,
        String operationId,
        String status,
        Long transferVersion,
        List<Line> lines
) {
    /** Kết quả đối soát đã ghi nhận cho một dòng của phiếu. */
    public record Line(String lineId, String sku, BigDecimal goodQuantity,
                       BigDecimal damagedQuantity, BigDecimal missingQuantity,
                       String discrepancyReason, String destinationInventoryId) {}
}
