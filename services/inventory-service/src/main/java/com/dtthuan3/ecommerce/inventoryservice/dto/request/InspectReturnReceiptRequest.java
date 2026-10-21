package com.dtthuan3.ecommerce.inventoryservice.dto.request;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** Dữ liệu phân loại toàn bộ số hàng của một parcel trả sau khi kiểm tra chất lượng. */
public record InspectReturnReceiptRequest(
        /** Khóa do client sinh để nhận diện lần kiểm tra và xử lý retry an toàn. */
        @NotBlank @Size(max = 120) String operationId,
        /** Số lượng đạt chuẩn, sẽ ra khỏi trạng thái chờ và trở thành khả dụng. */
        @NotNull @PositiveOrZero @Digits(integer = 15, fraction = 4) BigDecimal goodQuantity,
        /** Số lượng hỏng, sẽ chuyển từ bucket chờ sang bucket defective. */
        @NotNull @PositiveOrZero @Digits(integer = 15, fraction = 4) BigDecimal damagedQuantity,
        /** Lý do hoặc ghi chú kiểm tra để phục vụ truy vết. */
        @NotBlank @Size(max = 500) String reason
) {
}
