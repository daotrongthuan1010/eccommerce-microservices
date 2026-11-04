package com.dtthuan3.ecommerce.inventoryservice.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Thông tin shortage case để Order/Shipping tiếp tục quy trình đề xuất tái phân bổ.
 * Record này là hợp đồng dữ liệu trả về cho client. Service/controller chọn và tổng hợp các trường cần công
 * khai, giúp API không phụ thuộc trực tiếp vào cấu trúc entity lưu trong database.
 */
public record InventoryShortageCaseResponse(
        String id,
        String operationId,
        String enterpriseId,
        String warehouseId,
        String inventoryId,
        String orderId,
        String orderLineId,
        String allocationId,
        BigDecimal committedQuantity,
        BigDecimal dispatchedQuantity,
        BigDecimal shortageQuantity,
        String status,
        String reason,
        String reportedBy,
        LocalDateTime createdAt
) {}
