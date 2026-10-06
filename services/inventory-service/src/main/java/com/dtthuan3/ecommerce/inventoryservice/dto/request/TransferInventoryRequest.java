package com.dtthuan3.ecommerce.inventoryservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

/** Dữ liệu chuyển một SKU từ inventory nguồn sang inventory đích. */
public record TransferInventoryRequest(
        /** ID inventory bị trừ tồn. */
        @NotBlank String fromInventoryId,
        /** ID inventory được cộng tồn. */
        @NotBlank String toInventoryId,
        /** Số lượng phải dương và không vượt quá tồn khả dụng ở nguồn. */
        @NotNull @Positive BigDecimal quantity,
        /** Loại chứng từ/giao dịch liên quan; có thể bỏ trống. */
        String referenceType,
        /** Mã tham chiếu chung cho cả movement OUT và IN. */
        String referenceId,
        /** Lý do chuyển kho để phục vụ kiểm tra lịch sử. */
        String reason,
        /** Version nguồn client đã đọc; bỏ trống nếu không cần kiểm tra optimistic lock. */
        @PositiveOrZero Long expectedSourceVersion,
        /** Version đích client đã đọc; bỏ trống nếu không cần kiểm tra optimistic lock. */
        @PositiveOrZero Long expectedDestinationVersion
) {}
