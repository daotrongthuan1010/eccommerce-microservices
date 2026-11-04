package com.dtthuan3.ecommerce.inventoryservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/**
 * Nhập thêm hàng vào một inventory đã tồn tại theo ID.
 * Record này là hợp đồng dữ liệu đầu vào giữa client và Inventory. Các trường thể hiện thông tin cần cho
 * use case; validation ngăn dữ liệu thiếu hoặc sai trước khi service xử lý và record không đại diện cho
 * bảng database.
 */
public record ReceiveInventoryRequest(

        /**
         * ID của inventory cần nhập hàng.
         */
        @NotBlank(message = "inventoryId là bắt buộc")
        String inventoryId,

        /**
         * Số lượng hàng nhập vào.
         *
         * Phải > 0.
         */
        @NotNull(message = "quantity là bắt buộc")
        @Positive(message = "quantity phải lớn hơn 0")
        BigDecimal quantity,
        /** Version trả về lần đọc trước; bỏ trống nếu không cần kiểm tra dữ liệu cũ. */
        @PositiveOrZero(message = "expectedVersion phải lớn hơn hoặc bằng 0") Long expectedVersion
) {
}
