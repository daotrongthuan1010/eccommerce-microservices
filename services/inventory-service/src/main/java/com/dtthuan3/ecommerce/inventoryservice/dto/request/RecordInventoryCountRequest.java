package com.dtthuan3.ecommerce.inventoryservice.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;

import java.math.BigDecimal;
import java.util.List;

/** Gửi một lô số đếm vật lý cho các dòng baseline thuộc cùng phiên kiểm kê. */
public record RecordInventoryCountRequest(
        @NotBlank(message = "operationId không được để trống")
        @Size(max = 100, message = "operationId tối đa 100 ký tự") String operationId,
        @NotNull(message = "expectedVersion không được để trống")
        @Min(value = 0, message = "expectedVersion không được âm") Long expectedVersion,
        @NotEmpty(message = "Phải có ít nhất một dòng kiểm đếm")
        @Size(max = 200, message = "Mỗi lô tối đa 200 dòng kiểm đếm")
        List<@NotNull(message = "Dòng kiểm đếm không được để trống") @Valid Line> lines
) {
    /** Số đếm vật lý được chia thành hàng tốt, hàng hỏng và hàng đang chờ kiểm tra. */
    public record Line(
            @NotBlank(message = "inventoryId không được để trống")
            @Size(max = 36, message = "inventoryId tối đa 36 ký tự") String inventoryId,
            @NotNull(message = "countedGoodQuantity không được để trống")
            @DecimalMin(value = "0.0", message = "countedGoodQuantity không được âm")
            @Digits(integer = 15, fraction = 4, message = "countedGoodQuantity tối đa 4 số thập phân")
            BigDecimal countedGoodQuantity,
            @NotNull(message = "countedDefectiveQuantity không được để trống")
            @DecimalMin(value = "0.0", message = "countedDefectiveQuantity không được âm")
            @Digits(integer = 15, fraction = 4, message = "countedDefectiveQuantity tối đa 4 số thập phân")
            BigDecimal countedDefectiveQuantity,
            @NotNull(message = "countedPendingQuantity không được để trống")
            @DecimalMin(value = "0.0", message = "countedPendingQuantity không được âm")
            @Digits(integer = 15, fraction = 4, message = "countedPendingQuantity tối đa 4 số thập phân")
            BigDecimal countedPendingQuantity,
            @NotBlank(message = "countReason không được để trống")
            @Size(max = 500, message = "countReason tối đa 500 ký tự") String countReason,
            @Size(max = 500, message = "evidenceReference tối đa 500 ký tự") String evidenceReference
    ) {}
}
