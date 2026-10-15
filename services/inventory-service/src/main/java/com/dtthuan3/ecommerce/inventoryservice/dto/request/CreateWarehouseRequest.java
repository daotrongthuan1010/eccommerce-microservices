package com.dtthuan3.ecommerce.inventoryservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Dữ liệu tạo kho; bỏ id để Inventory tự sinh UUID, hoặc truyền id khi cần giữ mã kho sẵn có. */
public record CreateWarehouseRequest(
        /** ID tùy chọn để tương thích dữ liệu đã có; thông thường nên để trống. */
        @Size(max = 100, message = "id không được vượt quá 100 ký tự")
        String id,
        /** Doanh nghiệp sở hữu kho, dùng cùng code để bảo đảm mã kho không bị trùng trong doanh nghiệp. */
        @NotBlank(message = "enterpriseId là bắt buộc")
        @Size(max = 100, message = "enterpriseId không được vượt quá 100 ký tự")
        String enterpriseId,
        /** Mã ngắn cho người dùng; service chuẩn hóa thành chữ hoa trước khi lưu. */
        @NotBlank(message = "code là bắt buộc")
        @Size(max = 64, message = "code không được vượt quá 64 ký tự")
        @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9_-]*$", message = "code chỉ chứa chữ, số, _ hoặc -")
        String code,
        /** Tên đầy đủ hiển thị cho nhân viên quản lý kho. */
        @NotBlank(message = "name là bắt buộc")
        @Size(max = 200, message = "name không được vượt quá 200 ký tự")
        String name,
        /** Địa chỉ kho; trường tùy chọn. */
        @Size(max = 500, message = "address không được vượt quá 500 ký tự")
        String address
) {}
