package com.dtthuan3.ecommerce.paymentservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/** Audit + dedup cho IPN/callback: event_key unique dam bao 1 callback chi xu ly 1 lan. */
@Getter
@Setter
@Entity
@Table(name = "payment_callback_logs")
public class PaymentCallbackLog {
    @Id @Column(length = 36) private String id;
    @Column(nullable = false, length = 16) private String provider;
    @Column(name = "provider_txn_ref", length = 128) private String providerTxnRef;
    @Column(name = "event_key", nullable = false, unique = true, length = 64) private String eventKey;
    @Column(name = "signature_valid", nullable = false) private boolean signatureValid;
    @Column(nullable = false) private boolean processed;
    @Column(name = "raw_payload", columnDefinition = "TEXT") private String rawPayload;
    @Column(name = "received_at", nullable = false) private Instant receivedAt;

    @PrePersist void onCreate() { if (receivedAt == null) receivedAt = Instant.now(); }
}
