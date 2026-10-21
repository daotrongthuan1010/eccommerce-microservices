package com.dtthuan3.ecommerce.reviewservice.entity;

import com.dtthuan3.ecommerce.reviewservice.enums.OutboxStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "outbox_event",
        uniqueConstraints = @UniqueConstraint(name = "uk_outbox_event", columnNames = {"event_id"}),
        indexes = {
                @Index(name = "ix_outbox_aggregate", columnList = "aggregate_type, aggregate_id, aggregate_version"),
                @Index(name = "ix_outbox_status_created", columnList = "status, created_at")
        }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutboxEvent {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "CHAR(36)", updatable = false, nullable = false)
    private String id;

    @Column(name = "event_id", columnDefinition = "CHAR(36)", nullable = false) private String eventId;
    @Column(name = "aggregate_type", nullable = false) private String aggregateType;
    @Column(name = "aggregate_id", columnDefinition = "CHAR(36)", nullable = false) private String aggregateId;
    @Column(name = "aggregate_version", nullable = false) private Long aggregateVersion;
    @Column(name = "event_type", nullable = false) private String eventType;
    @Column(name = "schema_version", nullable = false) private Integer schemaVersion;
    @Column(columnDefinition = "JSON", nullable = false) private String payload;

    @Enumerated(EnumType.STRING) @Column(length = 20, nullable = false) private OutboxStatus status;
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
}
