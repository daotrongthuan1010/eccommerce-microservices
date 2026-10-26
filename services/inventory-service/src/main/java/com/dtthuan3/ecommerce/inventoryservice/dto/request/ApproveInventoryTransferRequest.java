package com.dtthuan3.ecommerce.inventoryservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** Dữ liệu duyệt phiếu điều chuyển; caller phải giữ nguyên khóa khi gửi lại sau timeout. */
public record ApproveInventoryTransferRequest(
        /** Khóa idempotency riêng cho thao tác duyệt phiếu. */
        @NotBlank(message = "operationId là bắt buộc")
        @Size(max = 120, message = "operationId tối đa 120 ký tự")
        String operationId,

        /** Version phiếu mà người duyệt đã kiểm tra. */
        @NotNull(message = "expectedVersion là bắt buộc")
        @PositiveOrZero(message = "expectedVersion phải lớn hơn hoặc bằng 0")
        Long expectedVersion,

        /** Lý do hoặc ghi chú kiểm duyệt để phục vụ audit. */
        @Size(max = 500, message = "reason tối đa 500 ký tự")
        String reason
) {}
