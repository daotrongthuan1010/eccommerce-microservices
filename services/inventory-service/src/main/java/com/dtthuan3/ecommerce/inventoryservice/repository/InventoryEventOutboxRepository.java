package com.dtthuan3.ecommerce.inventoryservice.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Cổng JDBC cho outbox sự kiện kho; schema do Hibernate quản lý từ entity tương ứng.
 * Interface này là ranh giới truy cập dữ liệu cho service, gom các thao tác đọc/ghi cần thiết. Cách thực
 * thi bằng JPA hay JDBC nằm trong repository/adapter, không đặt trong controller.
 */
public interface InventoryEventOutboxRepository {
    /** Ghi một batch event trong transaction đang ghi các movement. */
    void appendBatch(List<OutboxMessage> messages);

    /** Claim event sẵn sàng bằng FOR UPDATE SKIP LOCKED; mỗi claim nhận token lease riêng. */
    List<OutboxMessage> claimAvailable(int limit, Instant now, Instant leaseUntil);

    /** Đánh dấu broker đã nhận event nếu token vẫn thuộc claim hiện tại. */
    boolean markPublished(String eventId, String leaseToken, Instant publishedAt);

    /** Lên lịch retry có backoff nếu token vẫn thuộc claim hiện tại. */
    boolean scheduleRetry(String eventId, String leaseToken, Instant availableAt, String errorMessage);

    /** Tổng event chưa publish dùng cho backlog metric. */
    long countPending();

    /** Thời điểm tạo event pending cũ nhất để tính tuổi backlog. */
    Optional<Instant> oldestPendingCreatedAt();

    /** Xóa một lô record đã publish quá hạn để giới hạn thời gian khóa và lượng WAL mỗi câu lệnh. */
    int deletePublishedBefore(Instant cutoff, int limit);

    /**
     * Bản ghi tối thiểu publisher cần để gửi event mà không đọc lại entity.
     * Record này là projection/value object mang đúng các cột cần thiết giữa repository và service. Nó không
     * tạo quan hệ JPA; việc khóa, lưu và cập nhật dữ liệu vẫn được thực hiện bởi repository.
     */
    record OutboxMessage(String eventId, String topic, String eventType, String aggregateId,
                         String payload, int attempts, String leaseToken) {
        /** Event mới được ghi vào outbox chưa có lease cho đến khi publisher claim. */
        public OutboxMessage(String eventId, String topic, String eventType, String aggregateId,
                             String payload, int attempts) {
            this(eventId, topic, eventType, aggregateId, payload, attempts, null);
        }
    }
}
