package com.dtthuan3.ecommerce.inventoryservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** Xác nhận reservation sau khi Order đã nhận kết quả thanh toán hợp lệ. */
public record CommitInventoryReservationRequest(
        @NotBlank @Size(max = 120) String operationId,
        @NotBlank @Size(max = 100) String orderId,
        @NotBlank @Size(max = 150) String verifiedPaymentReference,
        @NotNull @PositiveOrZero Long expectedVersion
) {}
