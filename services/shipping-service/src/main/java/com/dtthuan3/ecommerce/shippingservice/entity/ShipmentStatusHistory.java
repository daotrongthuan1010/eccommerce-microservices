package com.dtthuan3.ecommerce.shippingservice.entity;

import com.dtthuan3.ecommerce.shippingservice.domain.ShipmentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "shipment_status_history", indexes = @Index(name = "idx_history_shipment", columnList = "shipment_id,occurred_at"))
public class ShipmentStatusHistory {
    @Id @Column(length = 36) private String id;
    @Column(name = "shipment_id", nullable = false, length = 36) private String shipmentId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 24) private ShipmentStatus status;
    @Column(length = 512) private String note;
    @Column(name = "occurred_at", nullable = false) private Instant occurredAt;

    @PrePersist void onCreate() { if (occurredAt == null) occurredAt = Instant.now(); }
}
