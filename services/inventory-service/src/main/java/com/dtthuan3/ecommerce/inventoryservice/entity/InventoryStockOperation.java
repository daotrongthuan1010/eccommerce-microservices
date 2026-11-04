package com.dtthuan3.ecommerce.inventoryservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Dấu idempotency cho các API nhập/xuất tồn trực tiếp.
 * Một operationId chỉ được ghi một lần; payload hash phát hiện client dùng lại khóa cho nghiệp vụ khác,
 * còn responseJson lưu snapshot để retry nhận đúng kết quả đã commit dù tồn kho đã thay đổi về sau.
 */
@Entity
@Table(name = "inventory_stock_operation", uniqueConstraints = {
        @UniqueConstraint(name = "uk_inventory_stock_operation_key", columnNames = "operation_id")
})
@Getter
@Setter
@NoArgsConstructor
public class InventoryStockOperation {

    /** Khóa nội bộ của bản ghi idempotency. */
    @Id
    @Column(name = "id", nullable = false, length = 36)
    private String id;

    /** Khóa client gửi qua header Idempotency-Key; unique ở database để chống request đồng thời. */
    @Column(name = "operation_id", nullable = false, length = 120)
    private String operationId;

    /** Phân biệt RECEIVE_STOCK, RECEIVE_EXISTING và ISSUE để không dùng một khóa lẫn nghiệp vụ. */
    @Column(name = "operation_type", nullable = false, length = 40)
    private String operationType;

    /** Inventory bị thay đổi; chỉ lưu ID đơn thuần, không tạo quan hệ JPA. */
    @Column(name = "inventory_id", length = 36)
    private String inventoryId;

    /** SHA-256 của dữ liệu nghiệp vụ chuẩn hóa, dùng phát hiện payload khác dưới cùng khóa. */
    @Column(name = "payload_hash", nullable = false, length = 64)
    private String payloadHash;

    /** Snapshot JSON phản hồi đã commit để trả lại nguyên kết quả khi client retry. */
    @Column(name = "response_json", columnDefinition = "text")
    private String responseJson;

    /** Thời điểm tạo operation để phục vụ audit và chính sách lưu giữ. */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
