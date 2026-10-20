package com.dtthuan3.ecommerce.reviewservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "inbox_event",
        uniqueConstraints = @UniqueConstraint(name = "uk_inbox_consumer_event", columnNames = {"consumer_name", "event_id"}),
        indexes = @Index(name = "ix_inbox_processed", columnList = "processed_at")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InboxEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "CHAR(36)", updatable = false, nullable = false)
    private String id;

    @Column(name = "consumer_name", length = 100, nullable = false)
    private String consumerName;
    @Column(name = "event_id", columnDefinition = "CHAR(36)", nullable = false)
    private String eventId;
    @Column(name = "processed_at", nullable = false)
    private LocalDateTime processedAt;
}