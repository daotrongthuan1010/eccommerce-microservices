package com.dtthuan3.ecommerce.inventoryservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * Dữ liệu để từ chối phiếu nhập đã gửi duyệt, gắn với version người duyệt đang xem.
 * Record này là hợp đồng dữ liệu đầu vào giữa client và Inventory. Các trường thể hiện thông tin cần cho
 * use case; validation ngăn dữ liệu thiếu hoặc sai trước khi service xử lý và record không đại diện cho
 * bảng database.
 */
public record RejectSupplierReceiptRequest(
        @NotNull(message = "expectedVersion là bắt buộc")
        @PositiveOrZero(message = "expectedVersion không được âm")
        Long expectedVersion,

        @NotBlank(message = "rejectionReason là bắt buộc")
        @Size(max = 500, message = "rejectionReason tối đa 500 ký tự")
        String rejectionReason
) {}
