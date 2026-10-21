package com.dtthuan3.ecommerce.inventoryservice.integration.event;

import java.time.Instant;

/**
 * Event phẳng do Product Service gửi để Inventory duy trì trạng thái SKU cục bộ.
 * Producer nên dùng skuCode làm Kafka message key và tăng version theo từng SKU để giữ thứ tự cập nhật.
 */
public record ProductSkuEvent(
        /** ID duy nhất của lần phát event, dùng truy vết log và đối soát. */
        String eventId,
        /** Loại event; Inventory hiện xử lý SkuCreated và SkuDisabled. */
        String eventType,
        /** Mã SKU làm khóa định danh projection trong Inventory. */
        String skuCode,
        /** Số phiên bản tăng dần của SKU tại Product Service để bỏ qua event cũ/lặp. */
        Long version,
        /** Thời điểm phát event ở nguồn; dùng làm thứ tự dự phòng khi thiếu version. */
        Instant occurredAt
) {
}
