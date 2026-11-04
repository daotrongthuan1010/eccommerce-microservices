package com.dtthuan3.ecommerce.inventoryservice.dto.response;

/**
 * Dữ liệu Location trả về qua ApiResponse; không trả entity JPA trực tiếp ra REST API.
 * Record này là hợp đồng dữ liệu trả về cho client. Service/controller chọn và tổng hợp các trường cần công
 * khai, giúp API không phụ thuộc trực tiếp vào cấu trúc entity lưu trong database.
 */
public record LocationResponse(String id, String warehouseId, String code, String name, boolean active) {
}
