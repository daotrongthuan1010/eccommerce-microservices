package com.dtthuan3.ecommerce.inventoryservice.dto.response;

import java.time.Instant;

/**
 * Dữ liệu kho công khai trả về API.
 * DTO tách khỏi entity để không vô tình lộ cấu trúc persistence khi schema thay đổi.
 */
public record WarehouseResponse(
        String id,
        String enterpriseId,
        String code,
        String name,
        String address,
        String status,
        Instant createdAt,
        Instant updatedAt
) {}
