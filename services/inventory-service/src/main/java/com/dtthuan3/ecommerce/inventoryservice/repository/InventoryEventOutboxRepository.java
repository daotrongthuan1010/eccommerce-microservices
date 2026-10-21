package com.dtthuan3.ecommerce.inventoryservice.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Cổng JDBC cho outbox sự kiện kho; schema do Hibernate quản lý từ entity tương ứng. */
public interface InventoryEventOutboxRepository {
    /** Ghi một batch event trong transaction đang ghi các movement. */
    void appendBatch(List<OutboxMessage> messages);

    /** Claim event sẵn sàng bằng FOR UPDATE SKIP LOCKED và lease để nhiều instance cùng poll an toàn. */
    List<OutboxMessage> claimAvailable(int limit, Instant now, Instant leaseUntil);

    /** Đánh dấu broker đã nhận event; trả false nếu dòng đã bị thay đổi/xóa. */
    boolean markPublished(String eventId, Instant publishedAt);

    /** Lên lịch retry có backoff sau lỗi gửi. */
    boolean scheduleRetry(String eventId, Instant availableAt, String errorMessage);

    /** Tổng event chưa publish dùng cho backlog metric. */
    long countPending();

    /** Thời điểm tạo event pending cũ nhất để tính tuổi backlog. */
    Optional<Instant> oldestPendingCreatedAt();

    /** Xóa record đã publish sau thời gian giữ lại để bảng outbox không tăng vô hạn. */
    int deletePublishedBefore(Instant cutoff);

    /** Bản ghi tối thiểu publisher cần để gửi event mà không đọc lại entity. */
    record OutboxMessage(String eventId, String topic, String eventType, String aggregateId,
                         String payload, int attempts) {
    }
}
