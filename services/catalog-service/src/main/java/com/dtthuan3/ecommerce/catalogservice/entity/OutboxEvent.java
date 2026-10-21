package com.dtthuan3.ecommerce.catalogservice.entity;

import com.dtthuan3.ecommerce.catalogservice.contstant.OutboxEventStatus;
import com.dtthuan3.ecommerce.catalogservice.generator.StringUuidSequenceGenerator;
import jakarta.persistence.*;
import lombok.Getter;
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
                @Index(name = "idx_outbox_events_event_id", columnList = "event_id"),
                @Index(name = "idx_outbox_events_status", columnList = "status"),
                @Index(name = "idx_outbox_events_occurred_at", columnList = "occurred_at")
        }
)
@Getter
@Setter
public class OutboxEvent extends BaseEntity {

    /** ID duy nhất của bản ghi outbox. */
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
    @Column(name = "id", nullable = false, updatable = false, length = 36)
    private String id;

    /** Loại aggregate phát sinh event. */
    @Column(name = "aggregate_type", nullable = false, length = 100)
    private String aggregateType;

    /** ID của aggregate phát sinh event. */
    @Column(name = "aggregate_id", nullable = false, length = 36)
    private String aggregateId;

    /** ID duy nhất của event. */
    @Column(name = "event_id", nullable = false, unique = true, length = 36)
    private String eventId;

    /** Tên loại event được phát ra. */
    @Column(name = "event_type", nullable = false, length = 150)
    private String eventType;

    /** Tên service phát sinh event. */
    @Column(name = "producer", nullable = false, length = 100)
    private String producer;

    /** Phiên bản schema của event. */
    @Column(name = "schema_version", nullable = false)
    private Integer schemaVersion;

    /** Payload của event được lưu dưới dạng JSONB. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> payload;

    /** Thời điểm event được tạo. */
    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    /** Thời điểm event được publish thành công. */
    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    /** Trạng thái publish của outbox event. */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private OutboxEventStatus status;

    /** Số lần thử publish lại. */
    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0;

    /** Lỗi publish gần nhất. */
    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;
}