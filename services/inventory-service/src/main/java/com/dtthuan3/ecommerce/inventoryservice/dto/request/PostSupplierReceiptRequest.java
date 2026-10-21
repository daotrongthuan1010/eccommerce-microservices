package com.dtthuan3.ecommerce.inventoryservice.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Dữ liệu gán Location cho từng dòng khi ghi sổ phiếu nhập đã kiểm đếm. */
public record PostSupplierReceiptRequest(
        /** Version hiện tại người thao tác đọc được; ngăn ghi sổ dựa trên phiếu đã thay đổi. */
        @NotNull(message = "expectedVersion là bắt buộc")
        @PositiveOrZero(message = "expectedVersion không được âm")
        Long expectedVersion,

        /** Phải chỉ định đúng một Location cho mọi dòng để không bỏ sót lượng thực nhận. */
        @NotEmpty(message = "Phải chỉ định Location cho các dòng phiếu")
        @Size(max = 500, message = "Phiếu không được vượt quá 500 dòng")
        List<@NotNull @Valid Line> lines
) {
    /** Gán vị trí chứa hàng cho một dòng phiếu cụ thể. */
    public record Line(
            /** ID dòng nhận từ API tạo phiếu hoặc API xem chi tiết. */
            @NotBlank(message = "lineId là bắt buộc")
            @Size(max = 36, message = "lineId không hợp lệ")
            String lineId,

            /** Location nội bộ đang hoạt động và phải thuộc Warehouse của phiếu nhập. */
            @NotBlank(message = "locationId là bắt buộc")
            @Size(max = 100, message = "locationId không được vượt quá 100 ký tự")
            String locationId
    ) {}
}
