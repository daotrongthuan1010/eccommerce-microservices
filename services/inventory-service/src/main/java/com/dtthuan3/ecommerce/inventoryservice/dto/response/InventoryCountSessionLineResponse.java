package com.dtthuan3.ecommerce.inventoryservice.dto.response;

import java.math.BigDecimal;

/** Một dòng baseline phân trang, dùng để đối chiếu tồn hệ thống với số lượng kiểm đếm thực tế. */
public record InventoryCountSessionLineResponse(
        String inventoryId,
        String sku,
        String locationId,
        BigDecimal physicalQuantity,
        BigDecimal reservedQuantity,
        BigDecimal defectiveQuantity,
        BigDecimal pendingQuantity,
        BigDecimal inTransitQuantity,
        Long inventoryVersion,
        BigDecimal countedGoodQuantity,
        BigDecimal countedDefectiveQuantity,
        BigDecimal countedPendingQuantity,
        String countReason,
        String evidenceReference,
        String countedBy
) {}
