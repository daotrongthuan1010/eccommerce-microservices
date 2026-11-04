package com.dtthuan3.ecommerce.inventoryservice.repository;

import java.time.Instant;
import java.util.Optional;

/**
 * Cổng dữ liệu chống xử lý lặp theo cặp consumer và eventId.
 * Interface này là ranh giới truy cập dữ liệu cho service, gom các thao tác đọc/ghi cần thiết. Cách thực
 * thi bằng JPA hay JDBC nằm trong repository/adapter, không đặt trong controller.
 */
public interface InventoryEventInboxRepository {
    /** Đăng ký event trong cùng transaction với cập nhật projection. */
    InboxClaim claim(String consumerName, String eventId, String payloadHash, Instant processedAt);

    /** Xóa một lô dấu inbox quá retention để giới hạn lock, WAL và thời lượng truy vấn. */
    int deleteProcessedBefore(Instant cutoff, int limit);

    /**
     * Kết quả claim event: inserted cho biết event mới; hash cũ giúp phát hiện eventId bị dùng sai payload.
     * Record này là projection/value object mang đúng các cột cần thiết giữa repository và service. Nó không
     * tạo quan hệ JPA; việc khóa, lưu và cập nhật dữ liệu vẫn được thực hiện bởi repository.
     */
    record InboxClaim(boolean inserted, String existingPayloadHash) {
    }
}
