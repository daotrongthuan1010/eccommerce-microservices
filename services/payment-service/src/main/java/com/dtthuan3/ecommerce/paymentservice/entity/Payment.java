package com.dtthuan3.ecommerce.paymentservice.entity;

import com.dtthuan3.ecommerce.paymentservice.domain.PaymentMethod;
import com.dtthuan3.ecommerce.paymentservice.domain.PaymentStatus;
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
@Table(name = "payments", indexes = {
        @Index(name = "idx_payments_order", columnList = "order_id"),
        @Index(name = "idx_payments_status_expires", columnList = "status,expires_at")})
public class Payment {
    @Id @Column(length = 36) private String id;
    @Column(name = "order_id", nullable = false, length = 64) private String orderId;
    @Column(name = "user_id", length = 64) private String userId;
    @Column(name = "idempotency_key", nullable = false, unique = true, length = 128) private String idempotencyKey;
    @Column(name = "request_hash", nullable = false, length = 64) private String requestHash;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16) private PaymentMethod method;
    @Column(nullable = false) private long amount;
    @Column(nullable = false, length = 3) private String currency = "VND";
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 24) private PaymentStatus status;
    @Column(name = "provider_txn_ref", unique = true, length = 128) private String providerTxnRef;
    @Column(name = "checkout_url", length = 1024) private String checkoutUrl;
    @Column(name = "qr_content", length = 1024) private String qrContent;
    @Column(name = "return_url", length = 1024) private String returnUrl;
    @Column(length = 512) private String description;
    @Column(name = "expires_at") private Instant expiresAt;
    @Column(name = "paid_at") private Instant paidAt;
    @Column(name = "refunded_amount", nullable = false) private long refundedAmount;
    @Column(name = "failure_code", length = 64) private String failureCode;
    @Column(name = "failure_message", length = 512) private String failureMessage;
    @Column(name = "late_payment", nullable = false) private boolean latePayment;
    @Version @Column(nullable = false) private long version;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    @PrePersist void onCreate() { Instant n = Instant.now(); createdAt = n; updatedAt = n; }
    @PreUpdate void onUpdate() { updatedAt = Instant.now(); }
}
