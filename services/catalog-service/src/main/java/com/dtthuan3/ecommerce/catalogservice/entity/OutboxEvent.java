package com.dtthuan3.ecommerce.catalogservice.entity;

import com.dtthuan3.ecommerce.catalogservice.contstant.OutboxEventStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "outbox_events",
        indexes = {
                @Index(
                        name = "idx_outbox_events_status",
                        columnList = "status"
                ),
                @Index(
                        name = "idx_outbox_events_event_type",
                        columnList = "event_type"
                ),
                @Index(
                        name = "idx_outbox_events_aggregate",
                        columnList = "aggregate_type, aggregate_id"
                ),
                @Index(
                        name = "idx_outbox_events_occurred_at",
                        columnList = "occurred_at"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutboxEvent extends BaseEntity {

    /**
     * ID của bản ghi Outbox
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Loại entity phát sinh event.
     *
     * Ví dụ:
     * CATEGORY
     * BRAND
     * MANUFACTURER
     */
    @Column(
            name = "aggregate_type",
            nullable = false,
            length = 100
    )
    private String aggregateType;

    /**
     * ID của entity phát sinh event.
     *
     * Ví dụ:
     * Category id = 10
     */
    @Column(
            name = "aggregate_id",
            nullable = false
    )
    private Long aggregateId;

    /**
     * ID duy nhất của event.
     *
     * Dùng để phân biệt từng event.
     */
    @Column(
            name = "event_id",
            nullable = false,
            unique = true,
            columnDefinition = "UUID"
    )
    private UUID eventId;

    /**
     * Loại event.
     *
     * Ví dụ:
     * CATEGORY_CREATED
     * CATEGORY_UPDATED
     * CATEGORY_DELETED
     */
    @Column(
            name = "event_type",
            nullable = false,
            length = 100
    )
    private String eventType;

    /**
     * Service tạo ra event.
     *
     * Ví dụ:
     * catalog-service
     */
    @Column(
            name = "producer",
            nullable = false,
            length = 100
    )
    private String producer;

    /**
     * Version của event schema.
     *
     * Ví dụ: 1
     */
    @Column(
            name = "schema_version",
            nullable = false
    )
    private Integer schemaVersion;

    /**
     * Nội dung event dạng JSON.
     *
     * Ví dụ:
     * {
     *   "id": 1,
     *   "name": "Điện thoại",
     *   "slug": "dien-thoai"
     * }
     */
    @Column(
            name = "payload",
            nullable = false,
            columnDefinition = "TEXT"
    )
    private String payload;

    /**
     * Thời điểm event được tạo.
     */
    @Column(
            name = "occurred_at",
            nullable = false
    )
    private LocalDateTime occurredAt;

    /**
     * Thời điểm event được publish thành công lên Kafka.
     *
     * NULL = chưa publish thành công.
     */
    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    /**
     * Trạng thái Outbox.
     *
     * PENDING
     * PUBLISHED
     * FAILED
     */
    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 30
    )
    private OutboxEventStatus status;

    /**
     * Số lần retry gửi Kafka.
     */
    @Column(
            name = "retry_count",
            nullable = false
    )
    private Integer retryCount = 0;

    /**
     * Lỗi gần nhất khi publish Kafka.
     */
    @Column(
            name = "last_error",
            columnDefinition = "TEXT"
    )
    private String lastError;
}