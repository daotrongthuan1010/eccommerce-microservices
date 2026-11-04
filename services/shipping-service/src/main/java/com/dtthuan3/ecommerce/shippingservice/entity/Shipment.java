package com.dtthuan3.ecommerce.shippingservice.entity;

import com.dtthuan3.ecommerce.shippingservice.domain.ServiceLevel;
import com.dtthuan3.ecommerce.shippingservice.domain.ShipmentStatus;
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
@Table(name = "shipments", indexes = {
        @Index(name = "idx_shipments_order", columnList = "order_id"),
        @Index(name = "idx_shipments_carrier_code", columnList = "carrier_code")})
public class Shipment {
    @Id @Column(length = 36) private String id;
    @Column(name = "order_id", nullable = false, length = 64) private String orderId;
    @Column(name = "idempotency_key", nullable = false, unique = true, length = 128) private String idempotencyKey;
    @Column(name = "request_hash", nullable = false, length = 64) private String requestHash;
    @Column(name = "quote_id", nullable = false, length = 36) private String quoteId;
    @Column(nullable = false, length = 16) private String carrier = "FAKE_GHN";
    @Column(name = "carrier_code", unique = true, length = 64) private String carrierCode;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 24) private ShipmentStatus status;
    @Enumerated(EnumType.STRING) @Column(name = "service_level", nullable = false, length = 16) private ServiceLevel serviceLevel;
    @Column(name = "shipping_fee", nullable = false) private long shippingFee;
    @Column(name = "cod_amount", nullable = false) private long codAmount;
    @Column(nullable = false, length = 3) private String currency = "VND";
    @Column(name = "weight_grams", nullable = false) private int weightGrams;

    @Column(name = "sender_name", length = 128) private String senderName;
    @Column(name = "sender_phone", length = 32) private String senderPhone;
    @Column(name = "sender_address", length = 512) private String senderAddress;
    @Column(name = "sender_province_code", length = 8) private String senderProvinceCode;

    @Column(name = "recipient_name", nullable = false, length = 128) private String recipientName;
    @Column(name = "recipient_phone", nullable = false, length = 32) private String recipientPhone;
    @Column(name = "recipient_address", nullable = false, length = 512) private String recipientAddress;
    @Column(name = "recipient_province_code", nullable = false, length = 8) private String recipientProvinceCode;

    @Column(name = "delivery_attempts", nullable = false) private int deliveryAttempts;
    @Column(name = "failure_reason", length = 512) private String failureReason;
    @Column(length = 512) private String note;
    @Version @Column(nullable = false) private long version;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    @PrePersist void onCreate() { Instant n = Instant.now(); createdAt = n; updatedAt = n; }
    @PreUpdate void onUpdate() { updatedAt = Instant.now(); }
}
