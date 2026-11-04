package com.dtthuan3.ecommerce.catalogservice.entity;

import com.dtthuan3.ecommerce.catalogservice.contstant.OutboxEventStatus;
import com.dtthuan3.ecommerce.catalogservice.generator.StringUuidSequenceGenerator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Parameter;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(
        name = "outbox_events",
        indexes = {
                @Index(
                        name = "idx_outbox_events_event_id",
                        columnList = "event_id"
                ),
                @Index(
                        name = "idx_outbox_events_status",
                        columnList = "status"
                ),
                @Index(
                        name = "idx_outbox_events_occurred_at",
                        columnList = "occurred_at"
                ),
                @Index(
                        name = "idx_outbox_events_aggregate",
                        columnList = "aggregate_type, aggregate_id"
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutboxEvent extends BaseEntity {

    /**
     * ID duy nhất của bản ghi outbox trong database.
     */
    @Id
    @GeneratedValue(generator = "outbox_event_uuid_generator")
    @GenericGenerator(
            name = "outbox_event_uuid_generator",
            type = StringUuidSequenceGenerator.class,
            parameters = {
                    @Parameter(
                            name = "sequence_name",
                            value = "outbox_events_id_seq"
                    )
            }
    )
    @Column(
            name = "id",
            nullable = false,
            updatable = false,
            length = 36
    )
    private String id;

    /**
     * Loại aggregate phát sinh event.
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
     * ID của aggregate phát sinh event.
     */
    @Column(
            name = "aggregate_id",
            nullable = false,
            length = 36
    )
    private String aggregateId;

    /**
     * Version của aggregate tại thời điểm phát sinh event.
     *
     * Dùng để đảm bảo event ordering và bỏ qua
     * event cũ ở consumer.
     */
    @Column(
            name = "aggregate_version",
            nullable = false
    )
    private Long aggregateVersion;

    /**
     * ID duy nhất của event.
     *
     * Dùng cho idempotency.
     */
    @Column(
            name = "event_id",
            nullable = false,
            unique = true,
            length = 36
    )
    private String eventId;

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
            length = 150
    )
    private String eventType;

    /**
     * Service phát sinh event.
     */
    @Column(
            name = "producer",
            nullable = false,
            length = 100
    )
    private String producer;

    /**
     * Version của schema event.
     *
     * Khác aggregateVersion:
     *
     * schemaVersion = version format của payload
     * aggregateVersion = version của aggregate.
     */
    @Column(
            name = "schema_version",
            nullable = false
    )
    private Integer schemaVersion;

    /**
     * Payload của event.
     *
     * PostgreSQL lưu dưới dạng JSONB.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(
            name = "payload",
            nullable = false,
            columnDefinition = "jsonb"
    )
    private Map<String, Object> payload;

    /**
     * Thời điểm event xảy ra.
     */
    @Column(
            name = "occurred_at",
            nullable = false
    )
    private LocalDateTime occurredAt;

    /**
     * Thời điểm Kafka publish thành công.
     */
    @Column(
            name = "published_at"
    )
    private LocalDateTime publishedAt;

    /**
     * Trạng thái của Outbox event.
     */
    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 30
    )
    private OutboxEventStatus status;

    /**
     * Số lần retry publish.
     */
    @Column(
            name = "retry_count",
            nullable = false
    )
    @Builder.Default
    private Integer retryCount = 0;

    /**
     * Lỗi publish gần nhất.
     */
    @Column(
            name = "last_error",
            columnDefinition = "TEXT"
    )
    private String lastError;
}