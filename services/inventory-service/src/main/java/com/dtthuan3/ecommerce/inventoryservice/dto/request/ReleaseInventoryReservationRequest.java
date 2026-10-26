package com.dtthuan3.ecommerce.inventoryservice.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/** Nhả toàn bộ residual hoặc các lượng cụ thể chưa dispatch của reservation. */
public record ReleaseInventoryReservationRequest(
        @NotBlank @Size(max = 120) String operationId,
        @NotBlank @Size(max = 200) String reason,
        @NotNull @PositiveOrZero Long expectedVersion,
        @Size(max = 200) List<@NotNull @Valid Line> lines
) {
    /** Lượng nhả trên một dòng; bỏ trống lines nghĩa là nhả toàn bộ residual. */
    public record Line(@NotBlank String reservationLineId,
                       @NotNull @Positive BigDecimal quantity) {}
}
