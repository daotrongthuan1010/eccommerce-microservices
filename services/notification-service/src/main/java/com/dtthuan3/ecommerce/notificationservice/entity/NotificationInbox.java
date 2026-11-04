package com.dtthuan3.ecommerce.notificationservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/** Inbox dedup: (eventId, source) — event Kafka den lai khong tao request lan 2. */
@Getter
@Setter
@Entity
@Table(name = "notification_inbox")
@IdClass(NotificationInboxId.class)
public class NotificationInbox {
    @Id @Column(name = "event_id", length = 36) private String eventId;
    @Id @Column(nullable = false, length = 64) private String source;
    @Column(name = "event_type", nullable = false, length = 64) private String eventType;
    @Column(name = "processed_at", nullable = false) private Instant processedAt;
}
