package com.dtthuan3.ecommerce.inventoryservice.dto.response;

import java.time.LocalDateTime;

/** Xác nhận một lô số đếm đã được ghi nhận và version mới của phiên. */
public record InventoryCountSubmissionResponse(
        String sessionId,
        String operationId,
        int linesRecorded,
        long version,
        LocalDateTime recordedAt
) {}
