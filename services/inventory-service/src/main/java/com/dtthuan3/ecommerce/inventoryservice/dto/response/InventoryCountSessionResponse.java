package com.dtthuan3.ecommerce.inventoryservice.dto.response;

import java.time.LocalDateTime;

/**
 * Kết quả mở phiên kiểm kê kèm số dòng tồn đã được chụp làm mốc so sánh.
 * Record này là hợp đồng dữ liệu trả về cho client. Service/controller chọn và tổng hợp các trường cần công
 * khai, giúp API không phụ thuộc trực tiếp vào cấu trúc entity lưu trong database.
 */
public record InventoryCountSessionResponse(
        String id,
        String enterpriseId,
        String warehouseId,
        String locationId,
        String sku,
        String status,
        String reason,
        String openedBy,
        LocalDateTime snapshotAt,
        long snapshotLineCount,
        long version
) {}
