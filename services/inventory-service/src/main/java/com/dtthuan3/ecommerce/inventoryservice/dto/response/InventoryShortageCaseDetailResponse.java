package com.dtthuan3.ecommerce.inventoryservice.dto.response;

import java.math.BigDecimal;
import java.util.List;

/**
 * Chi tiết shortage case kèm tiến độ giữ thay thế để Order/điều phối tiếp tục Saga.
 * Record này là hợp đồng dữ liệu trả về cho client. Service/controller chọn và tổng hợp các trường cần công
 * khai, giúp API không phụ thuộc trực tiếp vào cấu trúc entity lưu trong database.
 */
public record InventoryShortageCaseDetailResponse(
        InventoryShortageCaseResponse shortageCase,
        BigDecimal totalReallocatedQuantity,
        BigDecimal remainingShortageQuantity,
        List<ShortageReallocationReservationResponse> reallocations
) {}
