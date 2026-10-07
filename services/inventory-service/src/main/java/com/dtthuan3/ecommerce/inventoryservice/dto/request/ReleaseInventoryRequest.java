package com.dtthuan3.ecommerce.inventoryservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/** Yêu cầu bỏ giữ một lượng đã reserve trước đó. */
public record ReleaseInventoryRequest(

        /**
         * ID inventory cần bỏ giữ hàng.
         */
        @NotBlank(message = "Inventory id is required")
        String inventoryId,

        /**
         * Số lượng cần bỏ giữ.
         *
         * Phải lớn hơn 0.
         */
        @NotNull(message = "Quantity is required")
        @Positive(message = "Quantity must be greater than 0")
        BigDecimal quantity,
        /** Version client đã đọc; server trả 409 nếu inventory đã được cập nhật sau đó. */
        @PositiveOrZero Long expectedVersion
) {
}
