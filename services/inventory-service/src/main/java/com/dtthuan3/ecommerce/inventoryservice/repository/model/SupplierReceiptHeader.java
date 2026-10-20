package com.dtthuan3.ecommerce.inventoryservice.repository.model;

import java.time.Instant;

/**
 * Bản ghi dữ liệu tối giản của header phiếu nhập, dùng giữa repository JDBC và service.
 * Đây không phải JPA entity, không mang hành vi persistence và không bị Hibernate quản lý.
 */
public record SupplierReceiptHeader(
        /** UUID ổn định của phiếu nhập trong Inventory. */
        String id,
        /** ID kho nội bộ được database bảo vệ bằng khóa ngoại. */
        String warehouseId,
        /** Mã chứng từ/đơn hàng nhà cung cấp; NULL nếu caller chưa có mã này. */
        String supplierRef,
        /** Khóa idempotency duy nhất được dùng để tìm lại phiếu sau timeout. */
        String operationId,
        /** SHA-256 của nội dung request đã chuẩn hóa để phát hiện tái sử dụng khóa sai. */
        String payloadHash,
        /** Workflow state hiện tại; lúc tạo mới luôn là DRAFT. */
        String status,
        /** Phiên bản dữ liệu phục vụ optimistic concurrency ở API cập nhật. */
        long version,
        /** Thời điểm tạo chuẩn UTC, ánh xạ với PostgreSQL TIMESTAMPTZ. */
        Instant createdAt,
        /** Tài khoản ADMIN duyệt phiếu; null trước bước phê duyệt. */
        String approvedBy,
        /** Thời điểm duyệt phiếu theo UTC; null trước bước phê duyệt. */
        Instant approvedAt
) {}
