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

/** Dong gio hang; (cartId, skuId) unique — cung SKU trong gio chi co mot dong. */
@Getter
@Setter
@Entity
@Table(name = "cart_items")
public class CartItem {
    @Id @Column(name = "id", length = 36) private String id;
    @Column(name = "cart_id", nullable = false, length = 36) private String cartId;
    @Column(name = "sku_id", nullable = false, length = 64) private String skuId;
    @Column(name = "product_name", nullable = false, length = 255) private String productName;
    @Column(name = "unit_price", nullable = false) private long unitPrice;
    @Column(nullable = false) private int quantity;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    /** Thanh tien cua dong (tien VND so nguyen). */
    public long lineAmount() { return unitPrice * quantity; }

    @PrePersist void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate void onUpdate() { updatedAt = Instant.now(); }
}
