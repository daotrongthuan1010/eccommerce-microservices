package com.dtthuan3.ecommerce.inventoryservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Các trường được phép cập nhật; warehouseId và trạng thái được quản lý qua API riêng. */
public record UpdateLocationRequest(
        /** Mã Location mới, vẫn phải duy nhất trong Warehouse hiện tại. */
        @NotBlank(message = "code là bắt buộc")
        @Size(max = 64, message = "code không được dài quá 64 ký tự")
        @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9_-]*$",
                message = "code phải bắt đầu bằng chữ hoặc số và chỉ chứa chữ, số, dấu gạch dưới hoặc gạch ngang")
        String code,

        /** Tên hiển thị mới của Location. */
        @NotBlank(message = "name là bắt buộc")
        @Size(max = 200, message = "name không được dài quá 200 ký tự")
        String name
) {
}
