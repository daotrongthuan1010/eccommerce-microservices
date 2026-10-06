package com.dtthuan3.ecommerce.inventoryservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/** Yêu cầu xuất hàng từ phần tồn khả dụng. */
public record IssueInventoryRequest(

        /**
         * ID inventory cần xuất hàng.
         */
        @NotBlank(message = "Inventory id is required")
        String inventoryId,

        /**
         * Số lượng cần xuất khỏi kho.
         */
        @NotNull(message = "Quantity is required")
        @Positive(message = "Quantity must be greater than 0")
        BigDecimal quantity,
        /** Version client đã đọc; giúp phát hiện dữ liệu tồn đã thay đổi trước khi xử lý. */
        @PositiveOrZero Long expectedVersion
) {
}
