package com.dtthuan3.ecommerce.inventoryservice.dto.response;

import java.math.BigDecimal;

/**
 * Xác nhận lượng đã giữ tại Inventory thay thế và tiến độ bù shortage.
 * Record này là hợp đồng dữ liệu trả về cho client. Service/controller chọn và tổng hợp các trường cần công
 * khai, giúp API không phụ thuộc trực tiếp vào cấu trúc entity lưu trong database.
 */
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
