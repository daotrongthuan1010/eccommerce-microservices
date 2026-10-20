package com.dtthuan3.ecommerce.inventoryservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/** Durable Redis cache invalidation command, committed atomically with its Location change. */
@Entity
@Table(name = "cache_invalidation_outbox",
        indexes = @Index(name = "idx_cache_invalidation_available", columnList = "available_at, created_at"))
@Getter
@NoArgsConstructor
public class CacheInvalidationOutboxEvent {
    @Id
    @Column(nullable = false, updatable = false, length = 36)
    private String id;

    @Column(name = "cache_name", nullable = false, length = 120)
    private String cacheName;

    @Column(name = "cache_key", nullable = false, length = 300)
    private String cacheKey;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "available_at", nullable = false)
    private Instant availableAt;

    @Column(nullable = false)
    private int attempts;

    public CacheInvalidationOutboxEvent(String cacheName, String cacheKey) {
        this.id = UUID.randomUUID().toString();
        this.cacheName = cacheName;
        this.cacheKey = cacheKey;
        this.createdAt = Instant.now();
        this.availableAt = this.createdAt;
        this.attempts = 0;
    }

    public void retryAt(Instant retryAt) {
        this.attempts++;
        this.availableAt = retryAt;
    }
}
