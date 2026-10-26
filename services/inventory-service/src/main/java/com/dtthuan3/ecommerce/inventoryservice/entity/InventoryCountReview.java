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

/** Lịch sử quyết định yêu cầu đếm lại; mỗi lần xem xét được append, không ghi đè quyết định trước. */
@Entity
@Table(name = "inventory_count_review",
        uniqueConstraints = @UniqueConstraint(name = "uk_inventory_count_review_operation",
                columnNames = {"session_id", "operation_id"}),
        indexes = @Index(name = "idx_inventory_count_review_session", columnList = "session_id, version_after"))
@Getter
@Setter
@NoArgsConstructor
public class InventoryCountReview {
    /** UUID do service tạo cho bản ghi kiểm toán. */
    @Id
    @Column(nullable = false, length = 36)
    private String id;

    /** ID phiên kiểm kê; Java giữ chuỗi thuần, FK do database initializer tạo. */
    @Column(name = "session_id", nullable = false, length = 36)
    private String sessionId;

    /** Idempotency key do client cấp cho thao tác yêu cầu đếm lại. */
    @Column(name = "operation_id", nullable = false, length = 100)
    private String operationId;

    /** Hash payload để phát hiện dùng lại operationId với lý do/version khác. */
    @Column(name = "payload_hash", nullable = false, length = 64)
    private String payloadHash;

    /** Lý do yêu cầu recount để người thực hiện biết cần xác minh điều gì. */
    @Column(nullable = false, length = 500)
    private String reason;

    /** Subject/tên người duyệt phát hành yêu cầu đếm lại. */
    @Column(name = "requested_by", nullable = false, length = 255)
    private String requestedBy;

    /** Version phiên sau khi chuyển từ PENDING_APPROVAL về OPEN. */
    @Column(name = "version_after", nullable = false)
    private Long versionAfter;

    /** Thời điểm ghi quyết định theo giờ ứng dụng. */
    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;
}
