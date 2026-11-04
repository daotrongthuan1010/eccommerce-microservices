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

/** Gio hang ACTIVE duy nhat cho moi user (uk_carts_user_active). */
@Getter
@Setter
@Entity
@Table(name = "carts")
public class Cart {
    @Id @Column(name = "id", length = 36) private String id;
    @Column(name = "user_id", nullable = false, length = 64) private String userId;
    @Column(nullable = false, length = 16) private String status = "ACTIVE";
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    @PrePersist void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate void onUpdate() { updatedAt = Instant.now(); }
}
