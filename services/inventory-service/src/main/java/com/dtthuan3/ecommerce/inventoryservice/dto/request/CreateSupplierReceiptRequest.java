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

/**
 * Dữ liệu người dùng gửi để mở một phiếu nhập nhà cung cấp ở trạng thái DRAFT.
 * Payload này chỉ mô tả hàng dự kiến và chứng từ tham chiếu; chưa làm thay đổi số dư tồn kho.
 * Việc ghi nhận số lượng thực nhận, kiểm tra chứng từ và cộng tồn thuộc các API ở bước sau.
 */
public record CreateSupplierReceiptRequest(
        /** Mã đơn mua hoặc mã tham chiếu do nhà cung cấp cung cấp; có thể bỏ trống nếu chưa có. */
        @Size(max = 120, message = "supplierRef không được vượt quá 120 ký tự")
        String supplierRef,

        /** Danh sách SKU dự kiến nhập; mỗi dòng được lưu nguyên thứ tự để đối soát với phiếu giấy. */
        @NotEmpty(message = "Phiếu nhập phải có ít nhất một dòng hàng")
        @Size(max = 500, message = "Phiếu nhập không được vượt quá 500 dòng")
        List<@NotNull @Valid Line> lines,

        /** ID các tệp đã tải lên File Service; bước tạo nháp chỉ lưu liên kết, chưa xác nhận trạng thái READY. */
        @Size(max = 100, message = "Không được đính kèm quá 100 chứng từ")
        List<@NotBlank @Size(max = 160) String> documentAssetIds,

        /** Khóa nghiệp vụ ổn định do caller tạo và phải dùng lại khi retry cùng một yêu cầu. */
        @NotBlank(message = "operationId là bắt buộc")
        @Size(max = 120, message = "operationId không được vượt quá 120 ký tự")
        String operationId
) {
    /** Một dòng hàng dự kiến nhập; API này chưa ghi actualQty hoặc phân loại chất lượng. */
    public record Line(
            /** Mã SKU được đối chiếu với Product Service trước khi tạo phiếu. */
            @NotBlank(message = "skuId là bắt buộc")
            @Size(max = 100, message = "skuId không được vượt quá 100 ký tự")
            String skuId,

            /** Đơn vị tính do nghiệp vụ sử dụng, ví dụ EACH, BOX hoặc KG. */
            @NotBlank(message = "uom là bắt buộc")
            @Size(max = 32, message = "uom không được vượt quá 32 ký tự")
            String uom,

            /** Số lượng dự kiến; phải dương và phù hợp độ chính xác tối đa bốn chữ số thập phân. */
            @NotNull(message = "expectedQty là bắt buộc")
            @Positive(message = "expectedQty phải lớn hơn 0")
            @Digits(integer = 15, fraction = 4, message = "expectedQty tối đa 15 chữ số nguyên và 4 chữ số thập phân")
            BigDecimal expectedQty,

            /** Mã lô của nhà cung cấp nếu đã biết tại thời điểm mở phiếu. */
            @Size(max = 120, message = "lotCode không được vượt quá 120 ký tự")
            String lotCode
    ) {}
}
