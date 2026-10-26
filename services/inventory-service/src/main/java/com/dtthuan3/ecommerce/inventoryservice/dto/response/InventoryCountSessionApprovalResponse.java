package com.dtthuan3.ecommerce.inventoryservice.dto.response;

import java.math.BigDecimal;

/** Kết quả duyệt và áp dụng các bucket đã kiểm đếm vào Inventory. */
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
