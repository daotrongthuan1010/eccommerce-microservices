package com.dtthuan3.ecommerce.inventoryservice.integration.event;

import java.time.Instant;

/**
 * Event phẳng về trạng thái Warehouse do Warehouse Service gửi.
 * Producer nên đặt warehouseId làm Kafka message key và tăng version theo từng Warehouse.
 */
public record WarehouseStatusChangedEvent(
        /** ID duy nhất của lần phát event, phục vụ truy vết và đối soát. */
        String eventId,
        /** Loại event; Inventory chỉ xử lý WarehouseStatusChanged. */
        String eventType,
        /** ID Warehouse thuộc service khác; Inventory không tạo foreign key sang database đó. */
        String warehouseId,
        /** Enterprise sở hữu Warehouse, dùng kiểm tra dữ liệu tồn gửi lên có đúng phạm vi không. */
        String enterpriseId,
        /** Trạng thái nguồn, hiện chỉ chấp nhận ACTIVE hoặc INACTIVE. */
        String status,
        /** Số phiên bản tăng dần theo Warehouse để bỏ qua event đến trễ. */
        Long version,
        /** Thời điểm phát event ở nguồn; là mốc so sánh dự phòng nếu thiếu version. */
        Instant occurredAt
) {
}
