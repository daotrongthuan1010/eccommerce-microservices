package com.dtthuan3.ecommerce.catalogservice.web;

import java.math.BigDecimal;

/** Request tao/cap nhat san pham. */
public record ProductRequest(
        String sku,
        String name,
        String description,
        BigDecimal price,
        String currency,
        Integer stock,
        String status,
        String category) {}
