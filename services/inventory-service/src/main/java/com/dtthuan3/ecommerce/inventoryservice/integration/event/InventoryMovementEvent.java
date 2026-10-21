package com.dtthuan3.ecommerce.inventoryservice.integration.event;

import java.math.BigDecimal;
import java.time.Instant;

/** Sự kiện bất biến mô tả một movement đã commit; không chứa thông tin xác thực hoặc dữ liệu nhạy cảm. */
public record InventoryMovementEvent(
        /** Dùng cùng ID movement để consumer có thể khử trùng lặp. */
        String eventId,
        /** Tên hợp đồng sự kiện được version hóa. */
        String eventType,
        /** Thời điểm lập event ở service nguồn. */
        Instant occurredAt,
        /** ID bucket tồn chịu ảnh hưởng. */
        String inventoryId,
        /** Loại movement như RECEIVE, ISSUE hoặc TRANSFER_OUT. */
        String movementType,
        /** Lượng thay đổi được ghi nhận. */
        BigDecimal quantity,
        /** Tồn vật lý trước và sau giao dịch. */
        BigDecimal physicalBefore,
        BigDecimal physicalAfter,
        /** Tồn đã giữ trước và sau giao dịch. */
        BigDecimal reservedBefore,
        BigDecimal reservedAfter,
        /** Loại và ID chứng từ tham chiếu nếu có. */
        String referenceType,
        String referenceId
) {
}
