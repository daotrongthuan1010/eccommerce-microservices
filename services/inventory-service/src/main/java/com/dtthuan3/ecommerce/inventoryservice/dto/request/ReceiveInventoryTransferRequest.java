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

/** Dữ liệu xác nhận lượng tốt, hỏng và thất lạc khi nhận một đợt điều chuyển. */
public record ReceiveInventoryTransferRequest(
        /** Khóa chống áp dụng lặp cùng một lần nhận sau timeout. */
        @NotBlank(message = "operationId là bắt buộc")
        @Size(max = 120, message = "operationId tối đa 120 ký tự")
        String operationId,

        /** Version hiện tại của phiếu điều chuyển. */
        @NotNull(message = "expectedVersion là bắt buộc")
        @PositiveOrZero(message = "expectedVersion không được âm")
        Long expectedVersion,

        /** Có thể nhận một phần; các dòng không gửi lên vẫn tiếp tục ở trạng thái đang vận chuyển. */
        @NotEmpty(message = "Phải có ít nhất một dòng nhận hàng")
        @Size(max = 200, message = "Mỗi lần nhận tối đa 200 dòng")
        List<@Valid Line> lines
) {
    /** Kết quả kiểm đếm của một dòng điều chuyển trong lần nhận hiện tại. */
    public record Line(
            /** ID dòng do API tạo phiếu điều chuyển trả về. */
            @NotBlank(message = "lineId là bắt buộc")
            String lineId,

            /** Số hàng tốt thực nhận, được cộng vào tồn khả dụng ở kho đích. */
            @NotNull(message = "goodQuantity là bắt buộc")
            @PositiveOrZero(message = "goodQuantity không được âm")
            @Digits(integer = 15, fraction = 4, message = "goodQuantity tối đa 15 chữ số nguyên và 4 chữ số thập phân")
            BigDecimal goodQuantity,

            /** Số hàng hỏng thực nhận, được cộng vào bucket defective ở kho đích. */
            @NotNull(message = "damagedQuantity là bắt buộc")
            @PositiveOrZero(message = "damagedQuantity không được âm")
            @Digits(integer = 15, fraction = 4, message = "damagedQuantity tối đa 15 chữ số nguyên và 4 chữ số thập phân")
            BigDecimal damagedQuantity,

            /** Số hàng xác nhận thất lạc, được ghi nhận thành sai lệch và trừ khỏi lượng đang vận chuyển. */
            @NotNull(message = "missingQuantity là bắt buộc")
            @PositiveOrZero(message = "missingQuantity không được âm")
            @Digits(integer = 15, fraction = 4, message = "missingQuantity tối đa 15 chữ số nguyên và 4 chữ số thập phân")
            BigDecimal missingQuantity,

            /** Bắt buộc khi missingQuantity lớn hơn 0 để tạo hồ sơ sai lệch có căn cứ. */
            @Size(max = 500, message = "discrepancyReason tối đa 500 ký tự")
            String discrepancyReason
    ) {}
}
