package com.dtthuan3.ecommerce.orderservice.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Cap nhat so luong 1 dong gio hang; quantity=0 -> xoa dong. */
public record UpdateCartItemRequest(
        @NotNull @Min(0) @Max(999) int quantity) {
}
