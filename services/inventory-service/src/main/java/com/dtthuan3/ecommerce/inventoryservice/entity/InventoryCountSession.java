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
 * Phiên kiểm kê xác định phạm vi, người mở và snapshot tồn ban đầu.
 * Các ID được lưu dạng String, không có quan hệ JPA; khóa ngoại được bổ sung ở database khi khởi động.
 */
@Entity
@Table(name = "inventory_count_session",
        uniqueConstraints = @UniqueConstraint(name = "uk_inventory_count_operation",
                columnNames = {"enterprise_id", "operation_id"}),
        indexes = @Index(name = "idx_inventory_count_scope_status",
                columnList = "warehouse_id, location_id, status"))
@Getter
@Setter
@NoArgsConstructor
public class InventoryCountSession {
    /** UUID của phiên, được service tạo trước khi ghi dữ liệu. */
    @Id
    @Column(nullable = false, length = 36)
    private String id;

    /** Doanh nghiệp sở hữu phạm vi kiểm kê. */
    @Column(name = "enterprise_id", nullable = false, length = 100)
    private String enterpriseId;

    /** Kho được kiểm kê; chỉ lưu ID, khóa ngoại do database bảo đảm. */
    @Column(name = "warehouse_id", nullable = false, length = 36)
    private String warehouseId;

    /** Vị trí cụ thể; null nghĩa là toàn bộ vị trí trong kho. */
    @Column(name = "location_id", length = 36)
    private String locationId;

    /** SKU giới hạn phạm vi nếu có; null nghĩa là tất cả SKU trong phạm vi vị trí. */
    @Column(length = 100)
    private String sku;

    /** Khóa idempotency do caller gửi để retry không mở thêm phiên. */
    @Column(name = "operation_id", nullable = false, length = 100)
    private String operationId;

    /** SHA-256 của payload chuẩn hóa để từ chối dùng lại operationId với nội dung khác. */
    @Column(name = "payload_hash", nullable = false, length = 64)
    private String payloadHash;

    /** Trạng thái OPEN; các API đếm, duyệt và chốt sẽ tiến trạng thái ở các bước tiếp theo. */
    @Column(nullable = false, length = 30)
    private String status;

    /** Lý do kiểm kê để lưu dấu vết nghiệp vụ và hỗ trợ kiểm toán. */
    @Column(nullable = false, length = 500)
    private String reason;

    /** Người chịu trách nhiệm mở phiên kiểm kê. */
    @Column(name = "opened_by", nullable = false, length = 255)
    private String openedBy;

    /** Người duyệt cuối cùng; null khi phiên chưa qua bước phê duyệt. */
    @Column(name = "approved_by", length = 255)
    private String approvedBy;

    /** Thời điểm chuyển phiên sang APPROVED. */
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    /** Mốc snapshot được chụp cùng các số dư baseline. */
    @Column(name = "snapshot_at", nullable = false)
    private LocalDateTime snapshotAt;

    /** Optimistic version cho các API cập nhật trạng thái về sau. */
    @Column(nullable = false)
    private Long version;

    /** Thời điểm lưu phiên. */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
