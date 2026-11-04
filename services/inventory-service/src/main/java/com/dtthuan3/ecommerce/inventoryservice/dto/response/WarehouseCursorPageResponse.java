package com.dtthuan3.ecommerce.inventoryservice.dto.response;

import java.util.List;

/**
 * Trang cursor không cần COUNT(*) và chỉ trả cursor tiếp theo khi còn dữ liệu.
 * Record này là hợp đồng dữ liệu trả về cho client. Service/controller chọn và tổng hợp các trường cần công
 * khai, giúp API không phụ thuộc trực tiếp vào cấu trúc entity lưu trong database.
 */
public record WarehouseCursorPageResponse(
        List<WarehouseResponse> content,
        String nextCode,
        String nextId,
        boolean hasNext
) {}
