package com.dtthuan3.ecommerce.inventoryservice.dto.response;

import java.math.BigDecimal;

/** Xác nhận lượng đã giữ tại Inventory thay thế và tiến độ bù shortage. */
public record ShortageReallocationReservationResponse(
        String shortageCaseId,
        String operationId,
        String reallocationId,
        String targetInventoryId,
        BigDecimal reservedQuantity,
        BigDecimal totalReallocatedQuantity,
        BigDecimal remainingShortageQuantity,
        String shortageStatus,
        Long targetInventoryVersion
) {}
