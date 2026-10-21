package com.dtthuan3.ecommerce.productservice.domain;

import com.dtthuan3.ecommerce.productservice.constant.OutboxStatus;
import com.dtthuan3.ecommerce.productservice.mapper.GeneratedUuidV7;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "outbox_events",
        indexes = {
                @Index(
                        name = "idx_outbox_status_created",
                        columnList = "status, created_at"
                ),
                @Index(
                        name = "idx_outbox_aggregate",
                        columnList = "aggregate_type, aggregate_id"
                ),
                @Index(
                        name = "idx_outbox_next_retry",
                        columnList = "status, next_retry_at"
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutboxEvent extends BaseEntity {

    @Id
    @GeneratedUuidV7
    @Column(nullable = false, updatable = false)
    private UUID id;

    /**
     * ID duy nhất của event.
     */
    @Column(
            name = "event_id",
            nullable = false,
            unique = true,
            updatable = false
    )
    private UUID eventId;

    /**
     * Ví dụ:
     * PRODUCT
     * SKU
     * PRICE
     */
    @Column(
            name = "aggregate_type",
            nullable = false,
            length = 100
    )
    private String aggregateType;

    /**
     * ID của aggregate.
     */
    @Column(
            name = "aggregate_id",
            nullable = false
    )
    private UUID aggregateId;

    /**
     * Ví dụ:
     * ProductPublished
     * ProductUpdated
     * ProductSuspended
     * SkuAvailabilityChanged
     * PriceChanged
     */
    @Column(
            name = "event_type",
            nullable = false,
            length = 100
    )
    private String eventType;

    /**
     * Version của event schema.
     */
    @Column(
            name = "event_version",
            nullable = false
    )
    @Builder.Default
    private Integer eventVersion = 1;

    /**
     * Kafka topic.
     */
    @Column(
            nullable = false,
            length = 200
    )
    private String topic;

    /**
     * JSON payload gửi Kafka.
     */
    @Column(
            nullable = false,
            columnDefinition = "TEXT"
    )
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 30
    )
    @Builder.Default
    private OutboxStatus status = OutboxStatus.PENDING;

    @Column(
            name = "retry_count",
            nullable = false
    )
    @Builder.Default
    private Integer retryCount = 0;

    /**
     * Thời điểm retry tiếp theo.
     */
    @Column(name = "next_retry_at")
    private LocalDateTime nextRetryAt;

    /**
     * Thời điểm business event xảy ra.
     */
    @Column(
            name = "occurred_at",
            nullable = false
    )
    private LocalDateTime occurredAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;
}