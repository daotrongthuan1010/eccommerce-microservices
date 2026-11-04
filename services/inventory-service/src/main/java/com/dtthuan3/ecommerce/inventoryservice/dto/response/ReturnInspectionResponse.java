package com.dtthuan3.ecommerce.inventoryservice.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Kết quả kiểm tra parcel trả, bao gồm lượng được phân loại và version Inventory sau cập nhật.
 * Record này là hợp đồng dữ liệu trả về cho client. Service/controller chọn và tổng hợp các trường cần công
 * khai, giúp API không phụ thuộc trực tiếp vào cấu trúc entity lưu trong database.
 */
public record ReturnInspectionResponse(
        String inspectionId,
        String operationId,
        String receiptId,
        String inventoryId,
        String sku,
        BigDecimal receivedQuantity,
        BigDecimal goodQuantity,
        BigDecimal damagedQuantity,
        Long inventoryVersion,
        String status,
        String inspectedBy,
        LocalDateTime inspectedAt
) {
}
