package com.dtthuan3.ecommerce.inventoryservice.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Request gửi một phiếu nhập DRAFT sang bước chờ duyệt theo đúng phiên bản người dùng đã đọc.
 * Record này là hợp đồng dữ liệu đầu vào giữa client và Inventory. Các trường thể hiện thông tin cần cho
 * use case; validation ngăn dữ liệu thiếu hoặc sai trước khi service xử lý và record không đại diện cho
 * bảng database.
 */
public record SubmitSupplierReceiptRequest(
        /** Version hiện tại trên màn hình; server từ chối gửi nếu phiếu đã đổi sau lần đọc đó. */
        @NotNull(message = "expectedVersion là bắt buộc")
        @PositiveOrZero(message = "expectedVersion không được âm")
        Long expectedVersion
) {}
