package com.dtthuan3.ecommerce.inventoryservice.dto.response;

import java.time.LocalDateTime;

/**
 * Kết quả yêu cầu đếm lại; các observation cũ được giữ nguyên để tra cứu lịch sử.
 * Record này là hợp đồng dữ liệu trả về cho client. Service/controller chọn và tổng hợp các trường cần công
 * khai, giúp API không phụ thuộc trực tiếp vào cấu trúc entity lưu trong database.
 */
public record InventoryCountRecountResponse(
        String sessionId,
        String operationId,
        String status,
        long version,
        String reason,
        String requestedBy,
        LocalDateTime requestedAt
) {}
