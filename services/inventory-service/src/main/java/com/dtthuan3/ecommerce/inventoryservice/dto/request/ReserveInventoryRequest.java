package com.dtthuan3.ecommerce.inventoryservice.dto.request;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/**
 * Yêu cầu giữ trước một lượng hàng khả dụng của inventory.
 * Record này là hợp đồng dữ liệu đầu vào giữa client và Inventory. Các trường thể hiện thông tin cần cho
 * use case; validation ngăn dữ liệu thiếu hoặc sai trước khi service xử lý và record không đại diện cho
 * bảng database.
 */
public record ReserveInventoryRequest(

        /**
         * ID inventory cần giữ hàng.
         */
        @NotBlank(message = "inventoryId là bắt buộc")
        String inventoryId,

        /**
         * Số lượng cần giữ trước.
         *
         * Phải lớn hơn 0.
         */
        @NotNull(message = "quantity là bắt buộc")
        @Positive(message = "quantity phải lớn hơn 0")
        BigDecimal quantity,
        /** Version client đã đọc, dùng để phát hiện request dựa trên dữ liệu cũ. */
        @PositiveOrZero(message = "expectedVersion phải lớn hơn hoặc bằng 0") Long expectedVersion
) {
}
