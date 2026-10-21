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
 * Bản đồ Hibernate cho danh mục kho do Inventory sở hữu.
 * Các bảng Inventory, Location và phiếu nhập chỉ giữ warehouseId dạng scalar;
 * InventoryForeignKeyInitializer để PostgreSQL kiểm tra khóa ngoại mà không cần quan hệ JPA.
 */
@Entity
@Table(name = "inventory_warehouse",
        uniqueConstraints = @UniqueConstraint(name = "uk_inventory_warehouse_enterprise_code",
                columnNames = {"enterprise_id", "code"}),
        indexes = @Index(name = "idx_inventory_warehouse_enterprise_status",
                columnList = "enterprise_id, status"))
@Getter
@Setter
@NoArgsConstructor
public class Warehouse {
    /** UUID công khai dùng làm warehouseId trong các API và bảng nghiệp vụ liên quan. */
    @Id
    @Column(nullable = false, updatable = false, length = 100)
    private String id;

    /** Enterprise sở hữu kho; code chỉ cần duy nhất trong phạm vi enterprise này. */
    @Column(name = "enterprise_id", nullable = false, length = 100)
    private String enterpriseId;

    /** Mã kho phục vụ người vận hành tra cứu, được chuẩn hóa chữ hoa ở service. */
    @Column(nullable = false, length = 64)
    private String code;

    /** Tên hiển thị trên màn hình quản lý kho. */
    @Column(nullable = false, length = 200)
    private String name;

    /** Địa chỉ vật lý; được phép bỏ trống nếu kho chưa có thông tin địa chỉ. */
    @Column(length = 500)
    private String address;

    /** ACTIVE cho phép nhập/xuất; INACTIVE giữ lịch sử nhưng chặn thao tác mới. */
    @Column(nullable = false, length = 16)
    private String status;

    /** Thời điểm tạo kho, lưu UTC và không thay đổi khi cập nhật thông tin. */
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    /** Thời điểm gần nhất thông tin hoặc trạng thái kho được thay đổi. */
    @Column(nullable = false)
    private Instant updatedAt;
}
