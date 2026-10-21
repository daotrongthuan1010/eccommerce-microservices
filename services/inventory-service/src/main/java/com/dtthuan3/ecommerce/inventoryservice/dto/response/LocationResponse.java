package com.dtthuan3.ecommerce.inventoryservice.dto.response;

/** Dữ liệu Location trả về qua ApiResponse; không trả entity JPA trực tiếp ra REST API. */
public record LocationResponse(String id, String warehouseId, String code, String name, boolean active) {
}
