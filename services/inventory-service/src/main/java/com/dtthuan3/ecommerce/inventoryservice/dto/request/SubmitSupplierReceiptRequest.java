package com.dtthuan3.ecommerce.inventoryservice.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/** Request gửi một phiếu nhập DRAFT sang bước chờ duyệt theo đúng phiên bản người dùng đã đọc. */
public record SubmitSupplierReceiptRequest(
        /** Version hiện tại trên màn hình; server từ chối gửi nếu phiếu đã đổi sau lần đọc đó. */
        @NotNull(message = "expectedVersion là bắt buộc")
        @PositiveOrZero(message = "expectedVersion không được âm")
        Long expectedVersion
) {}
