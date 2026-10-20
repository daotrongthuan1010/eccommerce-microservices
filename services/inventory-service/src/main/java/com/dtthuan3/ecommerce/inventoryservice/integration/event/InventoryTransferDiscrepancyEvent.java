package com.dtthuan3.ecommerce.inventoryservice.integration.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Sự kiện outbox dành riêng cho bộ phận xử lý biên bản hàng điều chuyển bị thiếu. */
public record InventoryTransferDiscrepancyEvent(
        String eventId,
        String eventType,
        Instant occurredAt,
        String transferId,
        String operationId,
        List<Discrepancy> discrepancies
) {
    /** SKU, dòng phiếu, lượng thiếu và lý do cần được đối soát. */
    public record Discrepancy(String lineId, String sku, BigDecimal missingQuantity,
                              String discrepancyReason) {}
}
