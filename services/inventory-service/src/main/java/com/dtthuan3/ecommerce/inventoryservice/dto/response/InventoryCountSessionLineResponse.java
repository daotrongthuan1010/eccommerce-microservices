package com.dtthuan3.ecommerce.inventoryservice.dto.response;

import java.math.BigDecimal;

/**
 * Một dòng baseline phân trang, dùng để đối chiếu tồn hệ thống với số lượng kiểm đếm thực tế.
 * Record này là hợp đồng dữ liệu trả về cho client. Service/controller chọn và tổng hợp các trường cần công
 * khai, giúp API không phụ thuộc trực tiếp vào cấu trúc entity lưu trong database.
 */
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
