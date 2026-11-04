package com.dtthuan3.ecommerce.shippingservice.entity;

import com.dtthuan3.ecommerce.shippingservice.domain.ServiceLevel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/** Bao gia phi ship — co han dung 15 phut, chi dung duoc 1 lan (quote bi consume khi tao van don). */
@Getter
@Setter
@Entity
@Table(name = "shipping_quotes")
public class ShippingQuote {
    @Id @Column(length = 36) private String id;
    @Column(name = "from_province_code", nullable = false, length = 8) private String fromProvinceCode;
    @Column(name = "to_province_code", nullable = false, length = 8) private String toProvinceCode;
    @Column(name = "weight_grams", nullable = false) private int weightGrams;
    @Column(name = "billable_weight_grams", nullable = false) private int billableWeightGrams;
    @Column(name = "length_cm") private Integer lengthCm;
    @Column(name = "width_cm") private Integer widthCm;
    @Column(name = "height_cm") private Integer heightCm;
    @Enumerated(EnumType.STRING) @Column(name = "service_level", nullable = false, length = 16) private ServiceLevel serviceLevel;
    @Column(nullable = false, length = 16) private String zone;
    @Column(nullable = false) private long fee;
    @Column(nullable = false, length = 3) private String currency = "VND";
    @Column(name = "expires_at", nullable = false) private Instant expiresAt;
    @Column(name = "consumed_by_shipment_id", length = 36) private String consumedByShipmentId;
    @Version @Column(nullable = false) private long version;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    @PrePersist void onCreate() { Instant n = Instant.now(); createdAt = n; updatedAt = n; }
    @PreUpdate void onUpdate() { updatedAt = Instant.now(); }
}
