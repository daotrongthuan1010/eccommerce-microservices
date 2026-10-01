package com.dtthuan3.ecommerce.catalogservice.web;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** DTO san pham dung chung cho catalog-service (in-memory, chua can DB). */
public record ProductDto(
        String id,
        String sku,
        String name,
        String slug,
        String description,
        BigDecimal price,
        String currency,
        int stock,
        String status,
        String category,
        Instant createdAt,
        Instant updatedAt) {

    public static ProductDto of(String sku, String name, BigDecimal price, int stock) {
        Instant now = Instant.now();
        String safe = name == null ? "product" : name.trim().toLowerCase().replaceAll("[^a-z0-9]+", "-");
        return new ProductDto(
                UUID.randomUUID().toString(),
                sku,
                name,
                safe,
                null,
                price,
                "VND",
                stock,
                "ACTIVE",
                null,
                now,
                now);
    }
}
