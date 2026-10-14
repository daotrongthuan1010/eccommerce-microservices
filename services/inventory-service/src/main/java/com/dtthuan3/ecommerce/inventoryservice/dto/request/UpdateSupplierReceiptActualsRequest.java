package com.dtthuan3.ecommerce.inventoryservice.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * Dữ liệu kiểm đếm thực tế cho toàn bộ các dòng của một phiếu nhập nháp.
 * Request cập nhật số lượng nhận và phân loại chất lượng, nhưng chưa ghi tăng tồn kho.
 */
public record UpdateSupplierReceiptActualsRequest(
        /** Phiên bản phiếu mà người dùng đã đọc; dùng phát hiện cập nhật đồng thời. */
        @NotNull(message = "expectedVersion là bắt buộc")
        @PositiveOrZero(message = "expectedVersion không được âm")
        Long expectedVersion,

        /** Phải gửi đủ mỗi dòng hiện có đúng một lần để không vô tình xóa kết quả kiểm đếm trước đó. */
        @NotEmpty(message = "Phải cung cấp số liệu cho các dòng của phiếu")
        @Size(max = 500, message = "Phiếu không được vượt quá 500 dòng")
        List<@NotNull @Valid Line> lines,

        /** Tệp chứng minh việc giao nhận; các ID được nối thêm vào danh sách tài liệu của phiếu. */
        @Size(max = 100, message = "Không được đính kèm quá 100 chứng từ bổ sung")
        List<@NotBlank @Size(max = 160) String> evidenceAssetIds
) {
    /** Kết quả kiểm đếm và phân loại của một dòng phiếu. */
    public record Line(
            /** ID dòng do API tạo phiếu nháp trả về; không nhận lineNumber để tránh cập nhật nhầm dòng. */
            @NotBlank(message = "lineId là bắt buộc")
            @Size(max = 36, message = "lineId không hợp lệ")
            String lineId,

            /** Tổng số lượng thực tế nhà cung cấp giao; có thể bằng 0 nếu dòng hàng không được giao. */
            @NotNull(message = "actualQty là bắt buộc")
            @PositiveOrZero(message = "actualQty không được âm")
            @Digits(integer = 15, fraction = 4, message = "actualQty tối đa 15 chữ số nguyên và 4 chữ số thập phân")
            BigDecimal actualQty,

            /** Số lượng đã kiểm tra đạt chuẩn và có thể xem xét nhập vào hàng bán được khi duyệt. */
            @NotNull(message = "goodQty là bắt buộc")
            @PositiveOrZero(message = "goodQty không được âm")
            @Digits(integer = 15, fraction = 4, message = "goodQty tối đa 15 chữ số nguyên và 4 chữ số thập phân")
            BigDecimal goodQty,

            /** Số lượng kiểm tra xác định hỏng; chưa ghi sang bucket damaged ở API này. */
            @NotNull(message = "damagedQty là bắt buộc")
            @PositiveOrZero(message = "damagedQty không được âm")
            @Digits(integer = 15, fraction = 4, message = "damagedQty tối đa 15 chữ số nguyên và 4 chữ số thập phân")
            BigDecimal damagedQty,

            /** Số lượng đã nhận nhưng chưa có kết luận chất lượng, được giữ chờ kiểm tra. */
            @NotNull(message = "inspectionQty là bắt buộc")
            @PositiveOrZero(message = "inspectionQty không được âm")
            @Digits(integer = 15, fraction = 4, message = "inspectionQty tối đa 15 chữ số nguyên và 4 chữ số thập phân")
            BigDecimal inspectionQty
    ) {}
}
