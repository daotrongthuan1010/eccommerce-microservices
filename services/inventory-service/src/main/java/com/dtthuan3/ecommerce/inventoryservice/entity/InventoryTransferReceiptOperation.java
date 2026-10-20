package com.dtthuan3.ecommerce.inventoryservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/** Lưu khóa retry và snapshot trạng thái của từng lần nhận một phần phiếu điều chuyển. */
@Entity
@Table(name = "inventory_transfer_receipt_operation",
        uniqueConstraints = @UniqueConstraint(name = "uk_transfer_receipt_operation",
                columnNames = {"transfer_id", "operation_id"}),
        indexes = @Index(name = "idx_transfer_receipt_operation_transfer",
                columnList = "transfer_id, received_at"))
@Getter
@Setter
@NoArgsConstructor
public class InventoryTransferReceiptOperation {
    /** UUID nội bộ của lần nhận; operationId được unique theo transfer. */
    @Id
    @Column(nullable = false, length = 36, updatable = false)
    private String id;

    /** ID phiếu điều chuyển; chỉ lưu String, khóa ngoại do PostgreSQL kiểm tra. */
    @Column(name = "transfer_id", nullable = false, length = 36)
    private String transferId;

    /** Mã thao tác từ client dùng để trả lại đúng kết quả khi gửi lại request. */
    @Column(name = "operation_id", nullable = false, length = 120)
    private String operationId;

    /** Fingerprint request đã chuẩn hóa để phát hiện tái sử dụng operationId sai nội dung. */
    @Column(name = "payload_hash", nullable = false, length = 64)
    private String payloadHash;

    /** Trạng thái phiếu tại thời điểm lần nhận này hoàn tất. */
    @Column(name = "status_after", nullable = false, length = 24)
    private String statusAfter;

    /** Version phiếu sau khi ghi nhận lần nhận này. */
    @Column(name = "transfer_version_after", nullable = false)
    private Long transferVersionAfter;

    /** Nhân viên nhận hàng lấy từ JWT. */
    @Column(name = "received_by", length = 255)
    private String receivedBy;

    /** Thời điểm ghi nhận theo đồng hồ database. */
    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;
}
