package com.dtthuan3.ecommerce.inventoryservice.dto.request;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** Dữ liệu nhận một parcel thuộc dòng trả hàng đã được Order/CS duyệt. */
public record ReceiveReturnReceiptRequest(
        @NotBlank @Size(max = 100) String returnRequestId,
        @NotBlank @Size(max = 100) String orderLineId,
        @NotBlank @Size(max = 100) String parcelIdentity,
        @NotBlank @Size(max = 100) String enterpriseId,
        @NotBlank @Size(max = 36) String warehouseId,
        @NotBlank @Size(max = 36) String locationId,
        @NotBlank @Size(max = 100) String sku,
        @NotNull @Positive @Digits(integer = 15, fraction = 4) BigDecimal approvedQuantity,
        @NotNull @Positive @Digits(integer = 15, fraction = 4) BigDecimal receivedQuantity,
        @NotBlank @Size(max = 500) String reason
) {
}
