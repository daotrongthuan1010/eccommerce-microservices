package com.dtthuan3.ecommerce.inventoryservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Dữ liệu đầu vào để tạo Location; các ràng buộc được kiểm tra trước khi gọi service layer. */
public record CreateLocationRequest(
        /** ID tùy chọn do caller cung cấp; bỏ trống để Inventory tự sinh UUID. */
        @Size(max = 100, message = "id không được dài quá 100 ký tự") String id,
        /** ID tham chiếu Warehouse bên ngoài; Inventory không sở hữu hay tạo Warehouse. */
        @NotBlank(message = "warehouseId là bắt buộc")
        @Size(max = 100, message = "warehouseId không được dài quá 100 ký tự") String warehouseId,
        /** Mã Location trong phạm vi Warehouse; chỉ cho phép chữ/số và _ hoặc -. */
        @NotBlank(message = "code là bắt buộc")
        @Size(max = 64, message = "code không được dài quá 64 ký tự")
        @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9_-]*$",
                message = "code phải bắt đầu bằng chữ hoặc số và chỉ chứa chữ, số, dấu gạch dưới hoặc gạch ngang") String code,
        /** Tên hiển thị cho người vận hành. */
        @NotBlank(message = "name là bắt buộc")
        @Size(max = 200, message = "name không được dài quá 200 ký tự") String name
) {
}
