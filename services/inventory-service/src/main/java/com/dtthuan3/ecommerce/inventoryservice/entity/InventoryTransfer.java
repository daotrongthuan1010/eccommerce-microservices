package com.dtthuan3.ecommerce.inventoryservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Header của phiếu điều chuyển nhiều dòng giữa hai vị trí.
 * Entity chỉ ánh xạ cấu trúc để Hibernate tạo bảng; nghiệp vụ truy cập SQL qua repository JDBC.
 */
@Entity
@Table(name = "inventory_transfer",
        uniqueConstraints = @UniqueConstraint(name = "uk_inventory_transfer_operation", columnNames = "operation_id"),
        indexes = {
                @Index(name = "idx_inventory_transfer_source_status", columnList = "source_warehouse_id, status, created_at"),
                @Index(name = "idx_inventory_transfer_destination_status", columnList = "destination_warehouse_id, status, created_at")
        })
@Getter
@Setter
@NoArgsConstructor
public class InventoryTransfer {
    /** UUID do Inventory tạo, là khóa chính của phiếu. */
    @Id
    @Column(nullable = false, length = 36, updatable = false)
    private String id;

    /** Enterprise sở hữu cả hai kho; service xác minh ownership trước khi tạo phiếu. */
    @Column(name = "enterprise_id", nullable = false, length = 100)
    private String enterpriseId;

    /** ID kho nguồn; giữ dạng String, database có khóa ngoại mà không dùng quan hệ JPA. */
    @Column(name = "source_warehouse_id", nullable = false, length = 36)
    private String sourceWarehouseId;

    /** ID vị trí lấy hàng trong kho nguồn; chỉ lưu scalar ID. */
    @Column(name = "source_location_id", nullable = false, length = 36)
    private String sourceLocationId;

    /** ID kho nhận hàng; cơ sở dữ liệu bảo đảm kho này tồn tại. */
    @Column(name = "destination_warehouse_id", nullable = false, length = 36)
    private String destinationWarehouseId;

    /** ID vị trí nhận dự kiến; không tạo @ManyToOne tới InventoryLocation. */
    @Column(name = "destination_location_id", nullable = false, length = 36)
    private String destinationLocationId;

    /** Mã thao tác duy nhất dùng chống tạo phiếu lặp khi client gửi lại request. */
    @Column(name = "operation_id", nullable = false, length = 120)
    private String operationId;

    /** SHA-256 payload đã chuẩn hóa; phát hiện việc dùng lại operationId với dữ liệu khác. */
    @Column(name = "payload_hash", nullable = false, length = 64)
    private String payloadHash;

    /** Mã chứng từ của nghiệp vụ điều chuyển bên ngoài nếu có. */
    @Column(name = "reference_code", length = 120)
    private String referenceCode;

    /** Ghi chú của người tạo, hỗ trợ nhân viên kho hiểu mục đích chuyển. */
    @Column(length = 500)
    private String reason;

    /** CREATED ở API này; các API sau sẽ lần lượt xuất chuyển và xác nhận nhận. */
    @Column(nullable = false, length = 24)
    private String status;

    /** Tăng khi trạng thái phiếu chuyển đổi để API tiếp theo phát hiện dữ liệu cũ. */
    @Version
    @Column(nullable = false)
    private Long version;

    /** Người tạo lấy từ JWT, không nhận tùy ý từ request body. */
    @Column(name = "created_by", length = 255)
    private String createdBy;

    /** Thời điểm tạo phiếu UTC. */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** Khóa idempotency của lần dispatch; null khi phiếu mới chỉ ở CREATED. */
    @Column(name = "dispatch_operation_id", length = 120)
    private String dispatchOperationId;

    /** Version client gửi ở lần dispatch thành công để nhận diện retry đúng request. */
    @Column(name = "dispatch_expected_version")
    private Long dispatchExpectedVersion;

    /** Tài khoản đã bàn giao hàng cho vận chuyển, lấy từ JWT. */
    @Column(name = "dispatched_by", length = 255)
    private String dispatchedBy;

    /** Thời điểm chuyển trạng thái sang IN_TRANSIT theo UTC. */
    @Column(name = "dispatched_at")
    private Instant dispatchedAt;
}
