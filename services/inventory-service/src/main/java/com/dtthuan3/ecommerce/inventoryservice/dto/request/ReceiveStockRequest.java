package com.dtthuan3.ecommerce.inventoryservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/** Dữ liệu nhập hàng theo vị trí; service tạo inventory nếu tổ hợp vị trí/SKU chưa tồn tại. */
public record ReceiveStockRequest(

        /** Các trường tạo thành khóa nghiệp vụ của một vị trí tồn. */
        @NotBlank(message = "enterpriseId là bắt buộc")
        String enterpriseId,

        @NotBlank(message = "warehouseId là bắt buộc")
        String warehouseId,

        @NotBlank(message = "locationId là bắt buộc")
        String locationId,

        @NotBlank(message = "SKU là bắt buộc")
        String sku,

        @NotNull(message = "quantity là bắt buộc")
        @Positive(message = "quantity phải lớn hơn 0")
        BigDecimal quantity,

        /** Loại chứng từ dùng để đối soát lịch sử nhập. */
        String referenceType,

        /** Mã chứng từ/đơn hàng liên quan đến lần nhập này. */
        String referenceId,

        /** Lý do nhập, được lưu cùng movement. */
        String reason,

        /** Version client đã đọc; gửi lên để từ chối cập nhật nếu tồn đã đổi. */
        @PositiveOrZero(message = "expectedVersion phải lớn hơn hoặc bằng 0") Long expectedVersion

) {
}
