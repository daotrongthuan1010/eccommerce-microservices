package com.dtthuan3.ecommerce.inventoryservice.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Phiếu điều chuyển và các dòng hàng ở thời điểm mở phiếu. */
public record InventoryTransferResponse(
        /** ID của phiếu điều chuyển do Inventory tạo. */
        String transferId,
        /** Khóa idempotency để client tra lại kết quả khi retry. */
        String operationId,
        /** Trạng thái lúc tạo luôn là CREATED; chưa có hàng nào rời kho nguồn. */
        String status,
        /** Doanh nghiệp sở hữu phiếu và hai kho. */
        String enterpriseId,
        /** Kho và vị trí sẽ xuất hàng. */
        String sourceWarehouseId,
        String sourceLocationId,
        /** Kho và vị trí dự kiến nhận hàng. */
        String destinationWarehouseId,
        String destinationLocationId,
        /** Mã chứng từ do hệ thống gọi cung cấp, có thể null. */
        String referenceCode,
        /** Ghi chú nghiệp vụ, có thể null. */
        String reason,
        /** Phiên bản dùng để kiểm soát cạnh tranh ở các API điều chuyển tiếp theo. */
        Long version,
        /** Tài khoản đã mở phiếu, lấy từ ngữ cảnh xác thực. */
        String createdBy,
        /** Thời điểm mở phiếu theo UTC. */
        Instant createdAt,
        /** Các dòng theo thứ tự đã gửi lên. */
        List<Line> lines
) {
    /** Dòng hàng dự kiến trên phiếu; chưa biểu thị lượng đã xuất hoặc đã nhận. */
    public record Line(
            /** ID nội bộ của dòng phiếu. */
            String lineId,
            /** Số thứ tự hiển thị bắt đầu từ 1. */
            int lineNumber,
            /** Mã SKU cần điều chuyển. */
            String sku,
            /** Số lượng dự kiến chuyển. */
            BigDecimal plannedQuantity
    ) {}
}
