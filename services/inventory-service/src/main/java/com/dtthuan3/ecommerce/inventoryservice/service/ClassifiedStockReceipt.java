package com.dtthuan3.ecommerce.inventoryservice.service;

import java.math.BigDecimal;

/** Lệnh nội bộ chuyển kết quả phân loại của một dòng phiếu vào các bucket tồn kho. */
public record ClassifiedStockReceipt(
        String enterpriseId,
        String warehouseId,
        String locationId,
        String sku,
        BigDecimal goodQuantity,
        BigDecimal damagedQuantity,
        BigDecimal inspectionQuantity,
        String receiptId,
        String performedBy
) {}
