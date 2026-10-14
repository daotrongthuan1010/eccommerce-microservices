package com.dtthuan3.ecommerce.inventoryservice.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Kết quả các API trên phiếu nhập nháp. DRAFT cho biết phiếu chưa qua duyệt
 * và các số lượng chưa được ghi vào StockBalance hoặc StockLedger.
 */
public record SupplierReceiptResponse(
        /** ID phiếu nhập do Inventory tạo. */
        String receiptId,
        /** Kho nhận hàng được tham chiếu trong phiếu. */
        String warehouseId,
        /** Mã tham chiếu nhà cung cấp do caller gửi, nếu có. */
        String supplierRef,
        /** Trạng thái hiện tại; API này luôn trả DRAFT cho phiếu mới. */
        String status,
        /** Phiên bản dùng cho kiểm soát cập nhật đồng thời ở các API tiếp theo. */
        Long version,
        /** Khóa idempotency đã dùng để tạo hoặc tra lại kết quả. */
        String operationId,
        /** Danh sách dòng hàng theo đúng thứ tự khai báo trong request. */
        List<Line> lines,
        /** ID chứng từ được liên kết với phiếu. */
        List<String> documentAssetIds,
        /** Thời điểm phiếu được tạo theo UTC. */
        Instant createdAt
) {
    /** Chi tiết dự kiến, thực nhận và phân loại chất lượng của một dòng phiếu. */
    public record Line(
            /** ID do Inventory cấp cho dòng phiếu. */
            String lineId,
            /** Vị trí của dòng trong phiếu, bắt đầu từ 1. */
            int lineNumber,
            /** Mã SKU đã được xác thực khi tạo phiếu. */
            String skuId,
            /** Đơn vị tính của số lượng dự kiến. */
            String uom,
            /** Số lượng nhà cung cấp dự kiến giao. */
            BigDecimal expectedQty,
            /** Mã lô dự kiến nếu người tạo đã biết. */
            String lotCode,
            /** Tổng số lượng đã kiểm đếm thực nhận. */
            BigDecimal actualQty,
            /** Số lượng đã kiểm tra đạt chuẩn. */
            BigDecimal goodQty,
            /** Số lượng đã kết luận hỏng. */
            BigDecimal damagedQty,
            /** Số lượng còn chờ kiểm tra chất lượng. */
            BigDecimal inspectionQty
    ) {}
}
