package com.dtthuan3.ecommerce.inventoryservice.repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Cổng JDBC cho idempotency API nhập/xuất tồn trực tiếp.
 * Claim, cập nhật tồn và lưu response snapshot cùng transaction; unique key ở PostgreSQL phân xử request đồng thời.
 */
public interface InventoryStockOperationRepository {

    /** Tạo claim nếu khóa chưa tồn tại; false nghĩa là một request trước đã claim hoặc hoàn tất khóa này. */
    boolean tryStart(String operationId, String operationType, String inventoryId, String payloadHash,
                     LocalDateTime createdAt);

    /** Đọc kết quả của khóa đã tồn tại để trả response cũ hoặc phát hiện payload không khớp. */
    Optional<StockOperationRecord> findByOperationId(String operationId);

    /** Lưu snapshot response trước khi transaction commit; không cho cập nhật operation khác. */
    void complete(String operationId, String inventoryId, String responseJson);

    /** Projection JDBC tối giản cho dấu idempotency; không phải entity và không có quan hệ ORM. */
    record StockOperationRecord(String operationType, String inventoryId, String payloadHash, String responseJson) {}
}
