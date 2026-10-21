
package com.dtthuan3.ecommerce.productservice.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PriceScheduleResponse(
        UUID id,
        UUID skuId,
        BigDecimal price,
        String currency,
        LocalDateTime effectiveFrom,
        LocalDateTime effectiveTo,
        String status,
        Long productVersion
) {
}
