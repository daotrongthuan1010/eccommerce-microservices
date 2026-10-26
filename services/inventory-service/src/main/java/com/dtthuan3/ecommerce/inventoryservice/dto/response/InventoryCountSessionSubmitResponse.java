package com.dtthuan3.ecommerce.inventoryservice.dto.response;

import java.math.BigDecimal;

/** Tóm tắt kết quả kiểm kê khi chuyển phiên sang hàng đợi chờ người có thẩm quyền duyệt. */
public record InventoryCountSessionSubmitResponse(
        String sessionId,
        String status,
        long version,
        long countedLineCount,
        BigDecimal goodVariance,
        BigDecimal defectiveVariance,
        BigDecimal pendingVariance
) {}
