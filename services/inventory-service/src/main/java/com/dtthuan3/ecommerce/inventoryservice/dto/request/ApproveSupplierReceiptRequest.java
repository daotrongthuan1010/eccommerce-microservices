package com.dtthuan3.ecommerce.inventoryservice.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/** Request duyệt phiếu đã gửi, dựa trên đúng version mà người duyệt vừa xem. */
public record ApproveSupplierReceiptRequest(
        /** Version hiện tại; tránh duyệt dựa trên nội dung phiếu đã bị thay đổi. */
        @NotNull(message = "expectedVersion là bắt buộc")
        @PositiveOrZero(message = "expectedVersion không được âm")
        Long expectedVersion
) {}
