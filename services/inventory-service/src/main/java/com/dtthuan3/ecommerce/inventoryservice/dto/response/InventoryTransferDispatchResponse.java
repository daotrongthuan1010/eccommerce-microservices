package com.dtthuan3.ecommerce.inventoryservice.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Kết quả xuất chuyển; từng dòng đã rời tồn khả dụng và đang được theo dõi ở trạng thái in-transit. */
public record InventoryTransferDispatchResponse(
        /** ID phiếu điều chuyển đã được xuất. */
        String transferId,
        /** Trạng thái hiện tại của phiếu; ngay sau API dispatch là IN_TRANSIT. */
        String status,
        /** Khóa idempotency của lần xuất chuyển. */
        String operationId,
        /** Enterprise cùng hai đầu kho của phiếu. */
        String enterpriseId,
        String sourceWarehouseId,
        String sourceLocationId,
        String destinationWarehouseId,
        String destinationLocationId,
        /** Version mới sau khi chuyển CREATED sang IN_TRANSIT. */
        Long version,
        /** Người xác nhận xuất và thời điểm ghi nhận từ database. */
        String dispatchedBy,
        Instant dispatchedAt,
        /** Danh sách dòng đã được xuất cùng ID tồn nguồn để đối soát bước nhận. */
        List<Line> lines
) {
    /** Số lượng xuất và version tồn nguồn sau lần cập nhật. */
    public record Line(
            String lineId,
            int lineNumber,
            String sku,
            BigDecimal dispatchedQuantity,
            String sourceInventoryId,
            Long sourceInventoryVersionAfter
    ) {}
}
