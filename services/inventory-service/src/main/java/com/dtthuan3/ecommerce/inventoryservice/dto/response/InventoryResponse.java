package com.dtthuan3.ecommerce.inventoryservice.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Snapshot Inventory trả cho API, gồm số lượng theo từng trạng thái và version để đối soát cập nhật.
 * Record này là hợp đồng dữ liệu trả về cho client. Service/controller chọn và tổng hợp các trường cần công
 * khai, giúp API không phụ thuộc trực tiếp vào cấu trúc entity lưu trong database.
 */
@Builder
public record InventoryResponse(

        String id,

        String enterpriseId,

        String warehouseId,

        String locationId,

        String sku,

        BigDecimal physicalQuantity,

        BigDecimal reservedQuantity,

        BigDecimal defectiveQuantity,

        BigDecimal pendingQuantity,

        BigDecimal inTransitQuantity,

        BigDecimal availableQuantity,

        Long version,


        LocalDateTime createdAt,

        LocalDateTime updatedAt

) {
}
