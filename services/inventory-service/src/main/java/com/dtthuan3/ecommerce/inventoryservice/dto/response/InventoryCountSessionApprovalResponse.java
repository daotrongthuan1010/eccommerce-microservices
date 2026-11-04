package com.dtthuan3.ecommerce.inventoryservice.dto.response;

import java.math.BigDecimal;

/**
 * Kết quả duyệt và áp dụng các bucket đã kiểm đếm vào Inventory.
 * Record này là hợp đồng dữ liệu trả về cho client. Service/controller chọn và tổng hợp các trường cần công
 * khai, giúp API không phụ thuộc trực tiếp vào cấu trúc entity lưu trong database.
 */
public record InventoryCountSessionApprovalResponse(
        String sessionId,
        String status,
        long version,
        long processedLineCount,
        long adjustedLineCount,
        BigDecimal goodVariance,
        BigDecimal defectiveVariance,
        BigDecimal pendingVariance
) {}
