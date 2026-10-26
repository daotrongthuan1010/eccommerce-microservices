package com.dtthuan3.ecommerce.inventoryservice.dto.response;

import java.time.LocalDateTime;

/** Kết quả yêu cầu đếm lại; các observation cũ được giữ nguyên để tra cứu lịch sử. */
public record InventoryCountRecountResponse(
        String sessionId,
        String operationId,
        String status,
        long version,
        String reason,
        String requestedBy,
        LocalDateTime requestedAt
) {}
