package com.dtthuan3.ecommerce.inventoryservice.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/** Dữ liệu mở phiếu điều chuyển; thao tác tạo phiếu chưa làm thay đổi số dư tồn kho. */
public record CreateInventoryTransferRequest(
        /** Mã thao tác do client tạo và giữ nguyên khi retry để không tạo hai phiếu. */
        @NotBlank(message = "operationId là bắt buộc")
        @Size(max = 120, message = "operationId tối đa 120 ký tự")
        String operationId,

        /** Doanh nghiệp sở hữu cả kho nguồn và kho đích. */
        @NotBlank(message = "enterpriseId là bắt buộc") String enterpriseId,

        /** Kho xuất hàng; chỉ lưu ID chuỗi, database bảo đảm khóa ngoại. */
        @NotBlank(message = "sourceWarehouseId là bắt buộc") String sourceWarehouseId,

        /** Vị trí xuất hàng trong kho nguồn. */
        @NotBlank(message = "sourceLocationId là bắt buộc") String sourceLocationId,

        /** Kho sẽ nhận hàng sau khi phiếu được xuất chuyển và xác nhận nhận. */
        @NotBlank(message = "destinationWarehouseId là bắt buộc") String destinationWarehouseId,

        /** Vị trí dự kiến nhận hàng trong kho đích. */
        @NotBlank(message = "destinationLocationId là bắt buộc") String destinationLocationId,

        /** Mã chứng từ điều chuyển bên ngoài nếu doanh nghiệp có quy ước riêng. */
        @Size(max = 120, message = "referenceCode tối đa 120 ký tự")
        String referenceCode,

        /** Lý do hoặc ghi chú mở phiếu để nhân viên kho đối chiếu. */
        @Size(max = 500, message = "reason tối đa 500 ký tự")
        String reason,

        /** Các mặt hàng dự kiến chuyển; giới hạn để một request không tạo phiếu quá lớn. */
        @NotEmpty(message = "Phiếu điều chuyển phải có ít nhất một dòng hàng")
        @Size(max = 200, message = "Một phiếu điều chuyển tối đa 200 dòng hàng")
        List<@Valid Line> lines
) {
    /** Một SKU và số lượng dự kiến trên phiếu điều chuyển. */
    public record Line(
            /** SKU phải đang hoạt động trong Product Service. */
            @NotBlank(message = "sku là bắt buộc")
            @Size(max = 100, message = "sku tối đa 100 ký tự")
            String sku,

            /** Số lượng dự kiến phải dương và lưu tối đa bốn chữ số thập phân. */
            @NotNull(message = "quantity là bắt buộc")
            @Positive(message = "quantity phải lớn hơn 0")
            @Digits(integer = 15, fraction = 4, message = "quantity tối đa 15 chữ số nguyên và 4 chữ số thập phân")
            BigDecimal quantity
    ) {}
}
