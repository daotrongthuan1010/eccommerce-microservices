package com.dtthuan3.ecommerce.inventoryservice.dto.response;

import java.math.BigDecimal;
import java.util.List;

/** Chi tiết shortage case kèm tiến độ giữ thay thế để Order/điều phối tiếp tục Saga. */
public record InventoryShortageCaseDetailResponse(
        InventoryShortageCaseResponse shortageCase,
        BigDecimal totalReallocatedQuantity,
        BigDecimal remainingShortageQuantity,
        List<ShortageReallocationReservationResponse> reallocations
) {}
