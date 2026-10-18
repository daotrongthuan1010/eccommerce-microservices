package com.dtthuan3.ecommerce.inventoryservice.dto.request;

import jakarta.validation.constraints.NotNull;

/** Request bật/tắt Location; dùng Boolean để phân biệt false hợp lệ với trường active bị thiếu. */
public record SetActiveRequest(@NotNull(message = "active là bắt buộc") Boolean active) {
}
