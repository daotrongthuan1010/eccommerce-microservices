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

/**
 * Cấp quyền truy cập một kho cho một principal đã xác thực.
 * Quyền thao tác cụ thể (đọc, nhập, duyệt, ghi sổ, điều chỉnh, điều chuyển) vẫn do authority ở JWT kiểm tra;
 * bản ghi này giới hạn phạm vi dữ liệu theo warehouseId.
 */
@Entity
@Table(name = "inventory_warehouse_access_grant",
        uniqueConstraints = @UniqueConstraint(name = "uk_inventory_warehouse_access_subject",
                columnNames = {"warehouse_id", "subject_id"}),
        indexes = @Index(name = "idx_inventory_warehouse_access_subject", columnList = "subject_id, warehouse_id"))
@Getter
@Setter
@NoArgsConstructor
public class InventoryWarehouseAccessGrant {
    /** UUID cấp quyền, phục vụ audit và thu hồi chính xác. */
    @Id
    @Column(nullable = false, length = 36)
    private String id;

    /** ID kho nội bộ; khóa ngoại do database bảo đảm, không tạo quan hệ JPA. */
    @Column(name = "warehouse_id", nullable = false, length = 100)
    private String warehouseId;

    /** Subject ổn định của JWT, không dùng email/username có thể đổi. */
    @Column(name = "subject_id", nullable = false, length = 255)
    private String subjectId;

    /** Subject của quản trị viên đã cấp quyền. */
    @Column(name = "granted_by", nullable = false, length = 255)
    private String grantedBy;

    /** Mốc cấp quyền, dùng điều tra thay đổi quyền truy cập. */
    @Column(name = "granted_at", nullable = false)
    private Instant grantedAt;
}
