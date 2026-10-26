package com.dtthuan3.ecommerce.inventoryservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/** Bản ghi outbox bền vững để phát sự kiện kho sau khi transaction nghiệp vụ đã commit. */
@Entity
@Table(name = "inventory_event_outbox",
        indexes = {
                @Index(name = "idx_inventory_event_outbox_pending",
                        columnList = "published_at, available_at, created_at"),
                @Index(name = "idx_inventory_event_outbox_aggregate_order",
                        columnList = "aggregate_id, published_at, created_at, event_id")
        })
@Getter
@NoArgsConstructor
public class InventoryEventOutbox {
    /** ID sự kiện; với movement, ID này dùng lại ID movement để tránh tạo event trùng. */
    @Id
    @Column(name = "event_id", nullable = false, length = 36)
    private String eventId;

    /** Topic đích được chụp tại thời điểm ghi outbox. */
    @Column(nullable = false, length = 200)
    private String topic;

    /** Tên loại sự kiện để consumer định tuyến và theo dõi. */
    @Column(name = "event_type", nullable = false, length = 120)
    private String eventType;

    /** Khóa Kafka giúp giữ thứ tự sự kiện của cùng Inventory. */
    @Column(name = "aggregate_id", nullable = false, length = 100)
    private String aggregateId;

    /** Payload JSON đã được chụp lúc ghi movement. */
    @Column(nullable = false, columnDefinition = "text")
    private String payload;

    /** Thời điểm tạo sự kiện theo đồng hồ cơ sở dữ liệu. */
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /** Mốc sớm nhất publisher được phép thử gửi; đồng thời làm lease khi claim. */
    @Column(name = "available_at", nullable = false)
    private Instant availableAt;

    /** Token nhận diện lease hiện tại; callback của lease cũ không được sửa trạng thái mới. */
    @Column(name = "lease_token", length = 36)
    private String leaseToken;

    /** Số lần publisher đã claim để gửi. */
    @Column(nullable = false)
    private int attempts;

    /** Thời điểm broker xác nhận; null nghĩa là còn chờ xử lý hoặc retry. */
    @Column(name = "published_at")
    private Instant publishedAt;

    /** Lỗi gửi gần nhất đã rút gọn để chẩn đoán; không lưu token hay dữ liệu bí mật. */
    @Column(name = "last_error", columnDefinition = "text")
    private String lastError;
}
