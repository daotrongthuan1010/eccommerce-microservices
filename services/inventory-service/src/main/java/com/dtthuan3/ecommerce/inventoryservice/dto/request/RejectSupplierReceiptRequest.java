package com.dtthuan3.ecommerce.inventoryservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** Dữ liệu để từ chối phiếu nhập đã gửi duyệt, gắn với version người duyệt đang xem. */
public record RejectSupplierReceiptRequest(
        @NotNull(message = "expectedVersion là bắt buộc")
        @PositiveOrZero(message = "expectedVersion không được âm")
        Long expectedVersion,

        @NotBlank(message = "rejectionReason là bắt buộc")
        @Size(max = 500, message = "rejectionReason tối đa 500 ký tự")
        String rejectionReason
) {}
