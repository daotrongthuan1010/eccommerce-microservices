package com.dtthuan3.ecommerce.orderservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Chuyen trang thai don hang boi ADMIN (fulfill flow: PROCESSING/SHIPPED/DELIVERED/COMPLETED). */
public record AdminOrderStatusRequest(
        @NotBlank @Size(max = 24) String status) {
}
