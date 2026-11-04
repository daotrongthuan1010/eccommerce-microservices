package com.dtthuan3.ecommerce.inventoryservice.dto.response;

import java.math.BigDecimal;

/**
 * Tóm tắt kết quả kiểm kê khi chuyển phiên sang hàng đợi chờ người có thẩm quyền duyệt.
 * Record này là hợp đồng dữ liệu trả về cho client. Service/controller chọn và tổng hợp các trường cần công
 * khai, giúp API không phụ thuộc trực tiếp vào cấu trúc entity lưu trong database.
 */
public record InventoryCountSessionSubmitResponse(
        String sessionId,
        String status,
        long version,
        long countedLineCount,
        BigDecimal goodVariance,
        BigDecimal defectiveVariance,
        BigDecimal pendingVariance
) {}
