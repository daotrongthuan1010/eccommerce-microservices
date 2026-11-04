package com.dtthuan3.ecommerce.inventoryservice.service;

import java.math.BigDecimal;

/**
 * Lệnh nội bộ chuyển kết quả phân loại của một dòng phiếu vào các bucket tồn kho.
 * Record này gói các giá trị nghiệp vụ liên quan thành một kiểu có cấu trúc để các bước kiểm tra quyền/vị
 * trí truyền dữ liệu rõ ràng, không phải dựa vào chuỗi hoặc tham số rời.
 */
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
