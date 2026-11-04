package com.dtthuan3.ecommerce.inventoryservice.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * Lệnh xuất một phần/toàn bộ hàng đã commit cho một shipment.
 * Record này là hợp đồng dữ liệu đầu vào giữa client và Inventory. Các trường thể hiện thông tin cần cho
 * use case; validation ngăn dữ liệu thiếu hoặc sai trước khi service xử lý và record không đại diện cho
 * bảng database.
 */
public record DispatchInventoryReservationRequest(
        @NotBlank @Size(max = 120) String operationId,
        @NotBlank @Size(max = 120) String shipmentId,
        @NotBlank @Size(max = 100) String orderId,
        @NotNull @PositiveOrZero Long expectedVersion,
        @NotEmpty @Size(max = 200) List<@NotNull @Valid Line> lines
) {
    /**
     * Dòng shipment trỏ trực tiếp tới reservation line để không xuất trùng allocation.
     * Record này là hợp đồng dữ liệu đầu vào giữa client và Inventory. Các trường thể hiện thông tin cần cho
     * use case; validation ngăn dữ liệu thiếu hoặc sai trước khi service xử lý và record không đại diện cho
     * bảng database.
     */
    public record Line(@NotBlank String reservationLineId,
                       @NotNull @Positive BigDecimal quantity) {}
}
