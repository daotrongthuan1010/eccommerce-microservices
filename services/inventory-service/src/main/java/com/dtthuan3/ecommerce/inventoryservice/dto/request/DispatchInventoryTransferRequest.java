package com.dtthuan3.ecommerce.inventoryservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** Lệnh xuất toàn bộ các dòng của phiếu điều chuyển vào trạng thái đang vận chuyển. */
public record DispatchInventoryTransferRequest(
        /** Mã thao tác duy nhất; giữ nguyên cả mã và version khi gửi lại sau timeout. */
        @NotBlank(message = "operationId là bắt buộc")
        @Size(max = 120, message = "operationId tối đa 120 ký tự")
        String operationId,

        /** Version của phiếu nhận từ API tạo phiếu hoặc lần đọc gần nhất. */
        @NotNull(message = "expectedVersion là bắt buộc")
        @PositiveOrZero(message = "expectedVersion phải lớn hơn hoặc bằng 0")
        Long expectedVersion
) {}
