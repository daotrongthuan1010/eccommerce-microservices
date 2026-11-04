package com.dtthuan3.ecommerce.notificationservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/** DLQ co nguoi xu ly: NOTI-10 — nguyen nhan, trang thai, thao tac thu lai/cancel. */
@Getter
@Setter
@Entity
@Table(name = "notification_dead_letter",
        indexes = @Index(name = "idx_dl_request", columnList = "request_id"))
public class NotificationDeadLetter {
    @Id @Column(name = "id", length = 36) private String id;
    @Column(name = "request_id", nullable = false, length = 36) private String requestId;
    @Column(nullable = false, length = 64) private String reason;
    @Column(length = 512) private String detail;
    @Column(nullable = false, length = 24) private String status; // OPEN | RESOLVED
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "resolved_at") private Instant resolvedAt;
}
