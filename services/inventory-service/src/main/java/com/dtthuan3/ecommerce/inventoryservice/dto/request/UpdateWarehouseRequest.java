package com.dtthuan3.ecommerce.inventoryservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Request cập nhật thông tin hiển thị; enterprise sở hữu và trạng thái không đổi qua API này. */
public record UpdateWarehouseRequest(
        /** Mã kho mới, vẫn phải duy nhất trong enterprise đang sở hữu kho. */
        @NotBlank(message = "code là bắt buộc")
        @Size(max = 64, message = "code không được vượt quá 64 ký tự")
        @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9_-]*$", message = "code chỉ chứa chữ, số, _ hoặc -")
        String code,
        /** Tên hiển thị mới của kho. */
        @NotBlank(message = "name là bắt buộc")
        @Size(max = 200, message = "name không được vượt quá 200 ký tự")
        String name,
        /** Địa chỉ mới; gửi null để xóa địa chỉ đã lưu. */
        @Size(max = 500, message = "address không được vượt quá 500 ký tự")
        String address
) {}
