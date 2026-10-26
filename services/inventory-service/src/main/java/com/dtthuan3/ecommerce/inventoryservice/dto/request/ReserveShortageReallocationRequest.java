package com.dtthuan3.ecommerce.inventoryservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** Yêu cầu giữ một phần hoặc toàn bộ lượng thiếu tại Inventory thay thế. */
public record ReserveShortageReallocationRequest(
        /** Khóa idempotency riêng cho lần giữ kho thay thế này. */
        @NotBlank(message = "operationId là bắt buộc")
        @Size(max = 100, message = "operationId tối đa 100 ký tự")
        String operationId,

        /** ID định danh allocation mới do Order/Allocation workflow cấp. */
        @NotBlank(message = "reallocationId là bắt buộc")
        @Size(max = 100, message = "reallocationId tối đa 100 ký tự")
        String reallocationId,

        /** Inventory tại kho thay thế, cùng SKU và enterprise với shortage case. */
        @NotBlank(message = "targetInventoryId là bắt buộc")
        @Size(max = 36, message = "targetInventoryId tối đa 36 ký tự")
        String targetInventoryId,

        /** Lượng giữ tại kho thay; hỗ trợ chia shortage qua nhiều kho/vị trí. */
        @NotNull(message = "quantity là bắt buộc")
        @Positive(message = "quantity phải lớn hơn 0")
        BigDecimal quantity,

        /** Version Inventory kho thay mà caller đã đọc để phát hiện request dựa trên dữ liệu cũ. */
        @NotNull(message = "expectedVersion là bắt buộc")
        @PositiveOrZero(message = "expectedVersion không được âm")
        Long expectedVersion
) {}
