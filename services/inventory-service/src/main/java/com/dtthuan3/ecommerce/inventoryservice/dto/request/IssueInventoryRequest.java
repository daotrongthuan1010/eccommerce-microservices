package com.dtthuan3.ecommerce.inventoryservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/**
 * Yêu cầu xuất hàng từ phần tồn khả dụng.
 * Record này là hợp đồng dữ liệu đầu vào giữa client và Inventory. Các trường thể hiện thông tin cần cho
 * use case; validation ngăn dữ liệu thiếu hoặc sai trước khi service xử lý và record không đại diện cho
 * bảng database.
 */
public record IssueInventoryRequest(

        /**
         * ID inventory cần xuất hàng.
         */
        @NotBlank(message = "inventoryId là bắt buộc")
        String inventoryId,

        /**
         * Số lượng cần xuất khỏi kho.
         */
        @NotNull(message = "quantity là bắt buộc")
        @Positive(message = "quantity phải lớn hơn 0")
        BigDecimal quantity,
        /** Version client đã đọc; giúp phát hiện dữ liệu tồn đã thay đổi trước khi xử lý. */
        @NotNull
        @PositiveOrZero(message = "expectedVersion phải lớn hơn hoặc bằng 0") Long expectedVersion
) {
}
