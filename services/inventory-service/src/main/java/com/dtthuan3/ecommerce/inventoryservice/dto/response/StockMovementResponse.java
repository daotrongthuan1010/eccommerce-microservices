package com.dtthuan3.ecommerce.inventoryservice.dto.response;

import com.dtthuan3.ecommerce.inventoryservice.entity.MovementType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Dữ liệu lịch sử trả về; không trả entity để tránh tải quan hệ JPA không cần thiết. */
public record StockMovementResponse(
        /** ID tự tăng của bản ghi lịch sử. */
        String id,
        /** Inventory bị tác động. */
        String inventoryId,
        /** Loại nghiệp vụ đã làm thay đổi tồn. */
        MovementType movementType,
        /** Số lượng của nghiệp vụ. */
        BigDecimal quantity,
        /** Tồn vật lý và lượng đã giữ trước khi cập nhật. */
        BigDecimal physicalBefore,
        BigDecimal physicalAfter,
        BigDecimal reservedBefore,
        BigDecimal reservedAfter,
        /** Tham chiếu và lý do dùng để đối soát movement. */
        String referenceType,
        String referenceId,
        String reason,
        /** ID thao tác dạng số tương thích cũ và subject JWT đầy đủ. */
        String performedBy,
        String performedBySubject,
        LocalDateTime createdAt
) {}
