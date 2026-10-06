package com.dtthuan3.ecommerce.inventoryservice.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;


/** Một dòng tồn kho cho duy nhất một tổ hợp enterprise/warehouse/location/SKU. */
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(
        name = "inventory",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_inventory_location_sku",
                        columnNames = {
                                "enterprise_id",
                                "warehouse_id",
                                "location_id",
                                "sku"
                        }
                )
        },
        indexes = {
                @Index(name = "idx_inventory_sku", columnList = "sku"),
                @Index(name = "idx_inventory_warehouse", columnList = "warehouse_id"),
                @Index(name = "idx_inventory_location", columnList = "location_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Inventory {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private String id;

    /** Các ID phân vùng sở hữu và vị trí lưu SKU này. */
    @Column(name = "enterprise_id", nullable = false)
    private String enterpriseId;

    @Column(name = "warehouse_id", nullable = false)
    private String warehouseId;

    @Column(name = "location_id", nullable = false)
    private String locationId;

    @Column(name = "sku", nullable = false, length = 100)
    private String sku;

    /** Tổng số lượng vật lý đang có tại vị trí. */
    @Column(name = "physical_quantity", nullable = false, precision = 19, scale = 4)
    private BigDecimal physicalQuantity;

    @Column(name = "reserved_quantity", nullable = false, precision = 19, scale = 4)
    private BigDecimal reservedQuantity;

    @Column(name = "defective_quantity", nullable = false, precision = 19, scale = 4)
    private BigDecimal defectiveQuantity;

    @Column(name = "pending_quantity", nullable = false, precision = 19, scale = 4)
    private BigDecimal pendingQuantity;

    @Column(name = "in_transit_quantity", nullable = false, precision = 19, scale = 4)
    private BigDecimal inTransitQuantity;

    /** Tăng sau mỗi lần cập nhật để client phát hiện dữ liệu đã thay đổi kể từ lần đọc trước. */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    /** Thời điểm tạo do JPA Auditing gán một lần; không thay đổi khi cập nhật. */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Thời điểm sửa gần nhất do JPA Auditing quản lý khi entity được cập nhật qua JPA. */
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
