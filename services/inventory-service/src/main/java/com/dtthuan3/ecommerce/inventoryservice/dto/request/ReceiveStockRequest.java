package com.dtthuan3.ecommerce.inventoryservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/** Dữ liệu nhập hàng theo vị trí; service tạo inventory nếu tổ hợp vị trí/SKU chưa tồn tại. */
public record ReceiveStockRequest(

        /** Các trường tạo thành khóa nghiệp vụ của một vị trí tồn. */
        @NotBlank
        String enterpriseId,

        @NotBlank
        String warehouseId,

        @NotBlank
        String locationId,

        @NotBlank
        String sku,

        @NotNull
        @Positive
        BigDecimal quantity,

        /** Loại chứng từ dùng để đối soát lịch sử nhập. */
        String referenceType,

        /** Mã chứng từ/đơn hàng liên quan đến lần nhập này. */
        String referenceId,

        /** Lý do nhập, được lưu cùng movement. */
        String reason,

        /** Version client đã đọc; gửi lên để từ chối cập nhật nếu tồn đã đổi. */
        @PositiveOrZero Long expectedVersion

) {
}
