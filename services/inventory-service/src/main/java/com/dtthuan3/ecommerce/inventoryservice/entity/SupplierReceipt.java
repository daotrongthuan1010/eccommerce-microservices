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
 * Entity mapping để Hibernate tự tạo/cập nhật bảng header phiếu nhập theo ddl-auto: update.
 * Repository nghiệp vụ không nạp hoặc lưu entity này; các thao tác đọc/ghi dùng SQL/JdbcTemplate.
 */
@Entity
@Table(name = "inventory_supplier_receipt",
        uniqueConstraints = @UniqueConstraint(name = "uk_inventory_supplier_receipt_operation",
                columnNames = "operation_id"),
        indexes = @Index(name = "idx_inventory_supplier_receipt_warehouse_created",
                columnList = "warehouse_id, created_at"))
@Getter
@Setter
@NoArgsConstructor
public class SupplierReceipt {
    /** Khóa chính UUID do Inventory tạo cho phiếu nhập. */
    @Id
    @Column(nullable = false, updatable = false, length = 36)
    private String id;

    /** ID Warehouse nội bộ; không tạo quan hệ JPA, database giữ khóa ngoại trên header phiếu. */
    @Column(name = "warehouse_id", nullable = false, length = 100)
    private String warehouseId;

    /** Mã đơn mua/mã tham chiếu nhà cung cấp; có thể để trống khi mở phiếu nháp. */
    @Column(name = "supplier_ref", length = 120)
    private String supplierRef;

    /** Khóa chống xử lý lặp; unique constraint là chốt cuối cùng khi request đồng thời. */
    @Column(name = "operation_id", nullable = false, length = 120)
    private String operationId;

    /** SHA-256 của payload chuẩn hóa, dùng phát hiện operationId bị dùng cho request khác. */
    @Column(name = "payload_hash", nullable = false, length = 64)
    private String payloadHash;

    /** Trạng thái workflow; API tạo mới mở phiếu ở DRAFT và chưa thay đổi tồn kho. */
    @Column(nullable = false, length = 24)
    private String status;

    /** Phiên bản optimistic locking dành cho các API cập nhật phiếu ở bước sau. */
    @Version
    @Column(nullable = false)
    private Long version;

    /** Thời điểm tạo theo UTC; PostgreSQL/Hibernate ánh xạ Instant thành timestamp có múi giờ. */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
