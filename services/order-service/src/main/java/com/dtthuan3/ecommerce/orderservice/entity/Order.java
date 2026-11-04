package com.dtthuan3.ecommerce.orderservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/**
 * Don hang — aggregate goc cua order-service. Order la orchestrator duy nhat
 * (PAYMENT_SHIPPING_PLAN D1): payment/shipping khong goi nguoc order.
 * Trang thai: PENDING_PAYMENT -> PAID/CONFIRMED -> ... ; CANCELLED tu PENDING_PAYMENT.
 */
@Getter
@Setter
@Entity
@Table(name = "orders")
public class Order {
    @Id @Column(name = "id", length = 36) private String id;
    @Column(name = "order_code", nullable = false, length = 32) private String orderCode;
    @Column(name = "user_id", nullable = false, length = 64) private String userId;
    @Column(nullable = false, length = 24) private String status;
    @Column(name = "total_amount", nullable = false) private long totalAmount;
    @Column(nullable = false, length = 3) private String currency = "VND";
    @Column(name = "payment_method", length = 16) private String paymentMethod;
    @Column(name = "payment_id", length = 36) private String paymentId;
    @Column(name = "shipping_address", nullable = false, length = 512) private String shippingAddress;
    @Column(name = "recipient_name", nullable = false, length = 128) private String recipientName;
    @Column(name = "recipient_phone", nullable = false, length = 32) private String recipientPhone;
    @Column(name = "reservation_id", length = 100) private String reservationId;
    @Column(name = "idempotency_key", nullable = false, length = 128) private String idempotencyKey;
    @Column(name = "request_hash", nullable = false, length = 64) private String requestHash;
    @Column(name = "cancel_reason", length = 512) private String cancelReason;
    @Column(nullable = false) private long version;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    @PrePersist void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate void onUpdate() { updatedAt = Instant.now(); }
}
