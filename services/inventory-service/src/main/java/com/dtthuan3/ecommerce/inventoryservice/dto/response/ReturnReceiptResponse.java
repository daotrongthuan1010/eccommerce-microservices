package com.dtthuan3.ecommerce.inventoryservice.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Kết quả nhận hàng trả; số lượng được giữ trong pendingQuantity để chờ kiểm tra. */
public record ReturnReceiptResponse(
        String receiptId,
        String returnRequestId,
        String orderLineId,
        String parcelIdentity,
        String inventoryId,
        String sku,
        String warehouseId,
        String locationId,
        BigDecimal approvedQuantity,
        BigDecimal receivedQuantity,
        BigDecimal totalReceivedForLine,
        Long inventoryVersion,
        String status,
        LocalDateTime receivedAt
) {
}
