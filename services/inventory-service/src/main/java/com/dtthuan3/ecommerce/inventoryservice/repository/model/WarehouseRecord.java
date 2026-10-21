package com.dtthuan3.ecommerce.inventoryservice.repository.model;

import java.time.Instant;

/**
 * Bản ghi phẳng giữa JDBC repository và service.
 * Đây không phải entity JPA; repository chỉ đọc/ghi đúng các cột cần cho nghiệp vụ Warehouse.
 */
public record WarehouseRecord(
        String id,
        String enterpriseId,
        String code,
        String name,
        String address,
        String status,
        Instant createdAt,
        Instant updatedAt
) {}
