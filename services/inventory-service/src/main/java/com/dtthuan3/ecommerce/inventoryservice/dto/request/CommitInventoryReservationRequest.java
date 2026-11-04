package com.dtthuan3.ecommerce.inventoryservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * Xác nhận reservation sau khi Order đã nhận kết quả thanh toán hợp lệ.
 * Record này là hợp đồng dữ liệu đầu vào giữa client và Inventory. Các trường thể hiện thông tin cần cho
 * use case; validation ngăn dữ liệu thiếu hoặc sai trước khi service xử lý và record không đại diện cho
 * bảng database.
 */
public record CommitInventoryReservationRequest(
        @NotBlank @Size(max = 120) String operationId,
        @NotBlank @Size(max = 100) String orderId,
        @NotBlank @Size(max = 150) String verifiedPaymentReference,
        @NotNull @PositiveOrZero Long expectedVersion
) {}
