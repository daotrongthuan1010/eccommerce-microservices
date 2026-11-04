package com.dtthuan3.ecommerce.shippingservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/** Transactional outbox: ghi cung transaction voi shipment, poller day Kafka at-least-once. */
@Getter
@Setter
@Entity
@Table(name = "shipping_outbox", indexes = @Index(name = "idx_ship_outbox_pub", columnList = "published_at,created_at"))
public class ShippingOutbox {
    @Id @Column(name = "event_id", length = 36) private String eventId;
    @Column(name = "aggregate_id", nullable = false, length = 36) private String aggregateId;
    @Column(name = "event_type", nullable = false, length = 64) private String eventType;
    @Column(nullable = false, length = 128) private String topic;
    @Column(nullable = false, columnDefinition = "TEXT") private String payload;
    @Column(nullable = false) private int attempts;
    @Column(name = "published_at") private Instant publishedAt;
    @Column(name = "created_at", nullable = false) private Instant createdAt;

    @PrePersist void onCreate() { if (createdAt == null) createdAt = Instant.now(); }
}
