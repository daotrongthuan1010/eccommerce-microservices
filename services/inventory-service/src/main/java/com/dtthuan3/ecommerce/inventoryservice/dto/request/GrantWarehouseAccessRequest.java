package com.dtthuan3.ecommerce.inventoryservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Principal được quản trị viên cấp phạm vi truy cập vào một kho Inventory. */
public record GrantWarehouseAccessRequest(
        /** Subject UUID lấy từ claim sub của JWT, không dùng email làm định danh quyền. */
        @NotBlank @Size(max = 255) String subjectId
) {}
