package com.dtthuan3.ecommerce.inventoryservice.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Kết quả của một lần nhận hàng điều chuyển, gồm trạng thái phiếu và từng dòng đã đối soát. */
public record InventoryTransferReceiveResponse(
        String transferId,
        String operationId,
        String status,
        Long version,
        String receivedBy,
        Instant receivedAt,
        boolean hasDiscrepancy,
        List<Line> lines
) {
    /** Số lượng xác nhận trong lần nhận này và hai số dư dùng để truy vết. */
    public record Line(
            String lineId,
            String sku,
            BigDecimal goodQuantity,
            BigDecimal damagedQuantity,
            BigDecimal missingQuantity,
            String discrepancyReason,
            String sourceInventoryId,
            Long sourceInventoryVersionAfter,
            String destinationInventoryId,
            Long destinationInventoryVersionAfter
    ) {}
}
