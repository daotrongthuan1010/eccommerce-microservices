package com.dtthuan3.ecommerce.inventoryservice.repository;

import java.time.Instant;
import java.util.Optional;

/** Cổng dữ liệu chống xử lý lặp theo cặp consumer và eventId. */
public interface InventoryEventInboxRepository {
    /** Đăng ký event trong cùng transaction với cập nhật projection. */
    InboxClaim claim(String consumerName, String eventId, String payloadHash, Instant processedAt);

    /** Xóa dấu inbox cũ hơn retention đã cấu hình. */
    int deleteProcessedBefore(Instant cutoff);

    record InboxClaim(boolean inserted, String existingPayloadHash) {
    }
}
