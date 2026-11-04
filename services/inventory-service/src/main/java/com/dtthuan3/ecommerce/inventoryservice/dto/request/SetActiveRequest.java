package com.dtthuan3.ecommerce.inventoryservice.dto.request;

import jakarta.validation.constraints.NotNull;

/**
 * Request bật/tắt Location; dùng Boolean để phân biệt false hợp lệ với trường active bị thiếu.
 * Record này là hợp đồng dữ liệu đầu vào giữa client và Inventory. Các trường thể hiện thông tin cần cho
 * use case; validation ngăn dữ liệu thiếu hoặc sai trước khi service xử lý và record không đại diện cho
 * bảng database.
 */
public record SetActiveRequest(@NotNull(message = "active là bắt buộc") Boolean active) {
}
