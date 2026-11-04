package com.dtthuan3.ecommerce.inventoryservice.dto.response;

import java.time.LocalDateTime;

/**
 * Xác nhận một lô số đếm đã được ghi nhận và version mới của phiên.
 * Record này là hợp đồng dữ liệu trả về cho client. Service/controller chọn và tổng hợp các trường cần công
 * khai, giúp API không phụ thuộc trực tiếp vào cấu trúc entity lưu trong database.
 */
public record InventoryCountSubmissionResponse(
        String sessionId,
        String operationId,
        int linesRecorded,
        long version,
        LocalDateTime recordedAt
) {}
