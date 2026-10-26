package com.dtthuan3.ecommerce.inventoryservice.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Lệnh giữ hàng cho đơn; cả danh sách dòng được giữ toàn bộ hoặc rollback toàn bộ. */
public record CreateInventoryReservationRequest(
        /** ID reservation ổn định do Order cấp; giữ nguyên khi caller retry. */
        @NotBlank @Size(max = 100) String reservationId,
        /** Đơn sở hữu hàng giữ. */
        @NotBlank @Size(max = 100) String orderId,
        /** Idempotency key duy nhất cho lần giữ. */
        @NotBlank @Size(max = 120) String operationId,
        /** Hạn hết giữ theo UTC; phải lớn hơn thời điểm hiện tại. */
        @NotNull @Future Instant expiresAt,
        /** Các vị trí tồn được yêu cầu giữ; không cho phép lặp inventoryId trong một request. */
        @NotEmpty @Size(max = 200) List<@NotNull @Valid Line> lines
) {
    /** Một dòng hàng cần giữ từ đúng một inventory position. */
    public record Line(@NotBlank String inventoryId,
                       @NotNull @Positive BigDecimal quantity) {}
}
