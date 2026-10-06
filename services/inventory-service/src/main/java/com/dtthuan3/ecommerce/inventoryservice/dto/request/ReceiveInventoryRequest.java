package com.dtthuan3.ecommerce.inventoryservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/** Nhập thêm hàng vào một inventory đã tồn tại theo ID. */
public record ReceiveInventoryRequest(

        /**
         * ID của inventory cần nhập hàng.
         */
        @NotBlank(message = "Inventory id is required")
        String inventoryId,

        /**
         * Số lượng hàng nhập vào.
         *
         * Phải > 0.
         */
        @NotNull(message = "Quantity is required")
        @Positive(message = "Quantity must be greater than 0")
        BigDecimal quantity,
        /** Version trả về lần đọc trước; bỏ trống nếu không cần kiểm tra dữ liệu cũ. */
        @PositiveOrZero Long expectedVersion
) {
}
