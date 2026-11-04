package com.dtthuan3.ecommerce.paymentservice.entity;

import com.dtthuan3.ecommerce.paymentservice.domain.RefundStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "payment_refunds", indexes = @Index(name = "idx_refunds_payment", columnList = "payment_id"))
public class PaymentRefund {
    @Id @Column(length = 36) private String id;
    @Column(name = "payment_id", nullable = false, length = 36) private String paymentId;
    @Column(name = "idempotency_key", nullable = false, unique = true, length = 128) private String idempotencyKey;
    @Column(name = "request_hash", nullable = false, length = 64) private String requestHash;
    @Column(nullable = false) private long amount;
    @Column(length = 512) private String reason;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16) private RefundStatus status;
    @Column(name = "provider_refund_ref", length = 128) private String providerRefundRef;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @Version @Column(nullable = false) private long version;

    @PrePersist void onCreate() { Instant n = Instant.now(); createdAt = n; updatedAt = n; }
    @PreUpdate void onUpdate() { updatedAt = Instant.now(); }
}
