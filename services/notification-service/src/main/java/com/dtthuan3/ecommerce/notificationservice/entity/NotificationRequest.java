package com.dtthuan3.ecommerce.notificationservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/**
 * NotificationRequest: y dinh gui duoc luu ben vung truoc khi goi provider (NOTI-UC01 buoc 1).
 * businessKey = businessEventId|recipientId|channel|templateKey chong gui lap khi Kafka event lai (NOTI-03).
 * Trang thai: QUEUED -> SENDING -> ACCEPTED -> DELIVERED; FAILED_RETRYABLE/FAILED_FINAL/CANCELLED.
 * ACCEPTED khong tu dong thanh DELIVERED (NOTI-04).
 */
@Getter
@Setter
@Entity
@Table(name = "notification_request",
        indexes = {
                @Index(name = "idx_notification_due", columnList = "status, next_attempt_at"),
                @Index(name = "idx_notification_recipient", columnList = "recipient_id, created_at"),
                @Index(name = "idx_notification_order", columnList = "order_id")
        })
public class NotificationRequest {
    @Id @Column(name = "id", length = 36) private String id;
    @Column(name = "business_event_id", nullable = false, length = 64) private String businessEventId;
    @Column(name = "recipient_id", nullable = false, length = 64) private String recipientId;
    @Column(nullable = false, length = 16) private String channel;      // EMAIL | SMS
    @Column(name = "template_key", nullable = false, length = 64) private String templateKey;
    @Column(nullable = false, length = 16) private String purpose;      // TRANSACTIONAL | MARKETING
    @Column(name = "business_key", nullable = false, length = 255) private String businessKey;
    @Column(length = 255) private String subject;
    @Column(nullable = false, columnDefinition = "TEXT") private String body;
    @Column(nullable = false, length = 24) private String status;
    @Column(nullable = false) private int attempts;
    @Column(name = "next_attempt_at") private Instant nextAttemptAt;
    @Column(name = "provider_message_id", length = 128) private String providerMessageId;
    @Column(name = "last_error", length = 512) private String lastError;
    @Column(name = "order_id", length = 36) private String orderId;
    @Column(name = "correlation_id", length = 64) private String correlationId;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    @PrePersist void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate void onUpdate() { updatedAt = Instant.now(); }
}
