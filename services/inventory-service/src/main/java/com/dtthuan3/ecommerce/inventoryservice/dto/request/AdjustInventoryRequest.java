package com.dtthuan3.ecommerce.inventoryservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/** Dữ liệu điều chỉnh tồn theo kết quả kiểm kê thực tế. */
public record AdjustInventoryRequest(

        // ID inventory cần điều chỉnh.
        @NotBlank(message = "inventoryId là bắt buộc")
        String inventoryId,

        // Tổng tồn vật lý thực tế sau khi kiểm kê, cho phép bằng 0.
        @NotNull(message = "actualQuantity là bắt buộc")
        @PositiveOrZero(message = "actualQuantity phải lớn hơn hoặc bằng 0")
        BigDecimal actualQuantity,

        // Bắt buộc ghi lý do để người kiểm tra hiểu nguồn gốc điều chỉnh.
        @NotBlank(message = "reason là bắt buộc")
        String reason,
        // Version client đã đọc; nếu gửi version cũ thì service từ chối với HTTP 409.
        @PositiveOrZero(message = "expectedVersion phải lớn hơn hoặc bằng 0") Long expectedVersion

) {
}
