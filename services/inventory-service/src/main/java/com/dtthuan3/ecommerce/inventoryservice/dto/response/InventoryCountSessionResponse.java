package com.dtthuan3.ecommerce.inventoryservice.dto.response;

import java.time.LocalDateTime;

/** Kết quả mở phiên kiểm kê kèm số dòng tồn đã được chụp làm mốc so sánh. */
public record InventoryCountSessionResponse(
        String id,
        String enterpriseId,
        String warehouseId,
        String locationId,
        String sku,
        String status,
        String reason,
        String openedBy,
        LocalDateTime snapshotAt,
        long snapshotLineCount,
        long version
) {}
