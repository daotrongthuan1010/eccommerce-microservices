package com.dtthuan3.ecommerce.inventoryservice.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Request duyệt phiếu đã gửi, dựa trên đúng version mà người duyệt vừa xem.
 * Record này là hợp đồng dữ liệu đầu vào giữa client và Inventory. Các trường thể hiện thông tin cần cho
 * use case; validation ngăn dữ liệu thiếu hoặc sai trước khi service xử lý và record không đại diện cho
 * bảng database.
 */
public record ApproveSupplierReceiptRequest(
        /** Version hiện tại; tránh duyệt dựa trên nội dung phiếu đã bị thay đổi. */
        @NotNull(message = "expectedVersion là bắt buộc")
        @PositiveOrZero(message = "expectedVersion không được âm")
        Long expectedVersion
) {}
