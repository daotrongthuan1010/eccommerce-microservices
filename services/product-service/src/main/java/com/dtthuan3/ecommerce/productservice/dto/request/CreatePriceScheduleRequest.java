
package com.dtthuan3.ecommerce.productservice.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CreatePriceScheduleRequest(

        @NotNull(message = "Giá bán không được để trống")
        @DecimalMin(
                value = "0.00",
                inclusive = false,
                message = "Giá bán phải lớn hơn 0"
        )
        BigDecimal price,

        @Size(
                min = 3,
                max = 3,
                message = "Mã tiền tệ phải gồm 3 ký tự"
        )
        String currency,

        @NotNull(message = "Thời điểm bắt đầu hiệu lực không được để trống")
        LocalDateTime effectiveFrom,

        LocalDateTime effectiveTo,

        Long productVersion
) {
}
