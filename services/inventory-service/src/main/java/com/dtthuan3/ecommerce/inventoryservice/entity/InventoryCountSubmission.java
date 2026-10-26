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

import java.time.LocalDateTime;

/**
 * Một lô kiểm đếm được gửi lên; operationId và payloadHash giúp retry không ghi audit lặp.
 * ID phiên chỉ được lưu dạng String; database quản lý khóa ngoại, không dùng quan hệ JPA.
 */
@Entity
@Table(name = "inventory_count_submission",
        uniqueConstraints = @UniqueConstraint(name = "uk_inventory_count_submission_operation",
                columnNames = {"session_id", "operation_id"}),
        indexes = @Index(name = "idx_inventory_count_submission_session", columnList = "session_id, created_at"))
@Getter
@Setter
@NoArgsConstructor
public class InventoryCountSubmission {
    /** UUID của lô kiểm đếm. */
    @Id
    @Column(nullable = false, length = 36)
    private String id;

    /** Phiên kiểm kê nhận lô này; ID chuỗi, FK do database bảo đảm. */
    @Column(name = "session_id", nullable = false, length = 36)
    private String sessionId;

    /** Khóa idempotency duy nhất trong một phiên. */
    @Column(name = "operation_id", nullable = false, length = 100)
    private String operationId;

    /** Hash của request chuẩn hóa để phát hiện operationId bị dùng lại sai payload. */
    @Column(name = "payload_hash", nullable = false, length = 64)
    private String payloadHash;

    /** Số dòng được ghi trong lô. */
    @Column(name = "line_count", nullable = false)
    private Integer lineCount;

    /** Version phiên sau khi lô này được ghi thành công. */
    @Column(name = "version_after", nullable = false)
    private Long versionAfter;

    /** Người thực hiện đếm. */
    @Column(name = "recorded_by", nullable = false, length = 255)
    private String recordedBy;

    /** Thời điểm ghi nhận lô. */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
