package com.dtthuan3.ecommerce.notificationservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/**
 * Moi lan goi provider (NOTI-05/06). outcome: ACCEPTED|DELIVERED|FAILED_RETRYABLE|FAILED_FINAL|UNKNOWN.
 * UNKNOWN khong tu dong thanh cong — can tra provider truoc thu lai (NOTI-07).
 */
@Getter
@Setter
@Entity
@Table(name = "notification_delivery_attempt",
        indexes = @Index(name = "idx_attempt_request", columnList = "request_id, attempt_no"))
public class NotificationDeliveryAttempt {
    @Id @Column(name = "id", length = 36) private String id;
    @Column(name = "request_id", nullable = false, length = 36) private String requestId;
    @Column(name = "attempt_no", nullable = false) private int attemptNo;
    @Column(nullable = false, length = 24) private String outcome;
    @Column(name = "provider_message_id", length = 128) private String providerMessageId;
    @Column(name = "error_code", length = 64) private String errorCode;
    @Column(name = "error_message", length = 512) private String errorMessage;
    @Column(name = "attempted_at", nullable = false) private Instant attemptedAt;
}
