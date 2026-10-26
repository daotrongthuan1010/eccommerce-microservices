package com.dtthuan3.ecommerce.inventoryservice.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Thông tin shortage case để Order/Shipping tiếp tục quy trình đề xuất tái phân bổ. */
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
