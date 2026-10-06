package com.dtthuan3.ecommerce.inventoryservice.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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