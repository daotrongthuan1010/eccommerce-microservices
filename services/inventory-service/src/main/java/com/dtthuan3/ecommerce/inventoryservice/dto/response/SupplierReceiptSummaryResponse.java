package com.dtthuan3.ecommerce.inventoryservice.dto.response;

import java.time.Instant;

/** Tóm tắt phiếu nhập dùng cho danh sách; xem chi tiết từng dòng qua API GET theo receiptId. */
public record SupplierReceiptSummaryResponse(
        /** ID phiếu nhập trong Inventory. */
        String id,
        /** Kho nhận hàng của phiếu. */
        String warehouseId,
        /** Mã tham chiếu của nhà cung cấp nếu được cung cấp. */
        String supplierRef,
        /** Trạng thái hiện tại của phiếu, ví dụ DRAFT. */
        String status,
        /** Phiên bản dùng để phát hiện dữ liệu cũ khi cập nhật. */
        long version,
        /** Khóa idempotency đã dùng khi tạo phiếu. */
        String operationId,
        /** Số dòng SKU trên phiếu, không tải nội dung từng dòng trong API danh sách. */
        long lineCount,
        /** Thời điểm tạo phiếu theo UTC. */
        Instant createdAt
) {}
