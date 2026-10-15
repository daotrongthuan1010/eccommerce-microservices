package com.dtthuan3.ecommerce.inventoryservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Location thuộc Warehouse nội bộ; warehouseId là scalar ID với khóa ngoại do database quản lý. */
@Entity
@Table(name = "inventory_location",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_inventory_location_warehouse_code",
                columnNames = {"warehouse_id", "code"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryLocation {

    /** Khóa chính của Location; có thể do caller cấp hoặc Inventory tự sinh UUID. */
    @Id
    @Column(nullable = false, updatable = false, length = 100)
    private String id;

    /** ID Warehouse; không tạo quan hệ JPA, database giữ khóa ngoại từ inventory_location. */
    @Column(name = "warehouse_id", nullable = false, length = 100)
    private String warehouseId;

    /** Mã vị trí duy nhất trong một Warehouse, ví dụ A-01 hoặc RECEIVING. */
    @Column(nullable = false, length = 64)
    private String code;

    /** Tên hiển thị cho người dùng vận hành kho. */
    @Column(nullable = false, length = 200)
    private String name;

    /** false nghĩa là Location không được dùng cho thao tác ghi Inventory; bản ghi vẫn được giữ để bảo toàn lịch sử. */
    @Column(nullable = false)
    private boolean active;
}
