package com.dtthuan3.ecommerce.inventoryservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/** Dấu nhận sự kiện đã xử lý để Kafka redelivery không áp dụng lại thay đổi nghiệp vụ. */
@Entity
@Table(name = "inventory_event_inbox",
        uniqueConstraints = @UniqueConstraint(name = "uk_inventory_inbox_consumer_event",
                columnNames = {"consumer_name", "event_id"}),
        indexes = @Index(name = "idx_inventory_inbox_processed_at", columnList = "processed_at"))
@Getter
@NoArgsConstructor
public class InventoryEventInboxEntry {
    /** Khóa dòng nội bộ dạng String; tính duy nhất nghiệp vụ nằm ở consumer_name + event_id. */
    @Id
    @Column(nullable = false, length = 36)
    private String id;

    /** Tên consumer logic; cho phép từng projection có vùng chống trùng riêng. */
    @Column(name = "consumer_name", nullable = false, length = 120)
    private String consumerName;

    /** ID do producer tạo và phải giữ nguyên khi gửi lại cùng sự kiện. */
    @Column(name = "event_id", nullable = false, length = 200)
    private String eventId;

    /** Hash payload chuẩn hóa để phát hiện producer tái sử dụng eventId cho nội dung khác. */
    @Column(name = "payload_hash", nullable = false, length = 64)
    private String payloadHash;

    /** Thời điểm sự kiện được áp dụng atomically cùng projection. */
    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;
}
