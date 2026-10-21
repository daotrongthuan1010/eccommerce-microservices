package com.dtthuan3.ecommerce.inventoryservice.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;


/**
 * Bản ghi số dư của một SKU tại đúng một vị trí trong một doanh nghiệp.
 * Mỗi tổ hợp enterprise, warehouse, location và SKU chỉ có một dòng tồn kho.
 * Các ID được lưu trực tiếp, không tạo quan hệ JPA; ràng buộc khóa ngoại được
 * cơ sở dữ liệu bảo đảm để entity không phải tải các đối tượng liên quan.
 */
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

    /** ID duy nhất của dòng tồn; các API Inventory dùng giá trị này để tra cứu và cập nhật. */
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private String id;

    /** Doanh nghiệp sở hữu lượng tồn này; dùng để cô lập dữ liệu theo enterprise. */
    @Column(name = "enterprise_id", nullable = false)
    private String enterpriseId;

    /** ID kho chứa SKU; chỉ lưu ID, không ánh xạ quan hệ JPA tới Warehouse. */
    @Column(name = "warehouse_id", nullable = false)
    private String warehouseId;

    /** ID vị trí cụ thể trong kho; kết hợp với SKU để xác định dòng tồn. */
    @Column(name = "location_id", nullable = false)
    private String locationId;

    /** Mã SKU được theo dõi tại vị trí này, thường được xác thực với Product Service. */
    @Column(name = "sku", nullable = false, length = 100)
    private String sku;

    /** Tổng số lượng vật lý đang có tại vị trí. */
    @Column(name = "physical_quantity", nullable = false, precision = 19, scale = 4)
    private BigDecimal physicalQuantity;

    /** Lượng đã được giữ chỗ cho đơn hàng hoặc nghiệp vụ khác, chưa xuất khỏi kho. */
    @Column(name = "reserved_quantity", nullable = false, precision = 19, scale = 4)
    private BigDecimal reservedQuantity;

    /** Lượng hư hỏng đã xác định; được theo dõi riêng để không coi là hàng bán tốt. */
    @Column(name = "defective_quantity", nullable = false, precision = 19, scale = 4)
    private BigDecimal defectiveQuantity;

    /** Lượng đang chờ kiểm tra/chốt chất lượng; chưa được đưa vào tồn khả dụng. */
    @Column(name = "pending_quantity", nullable = false, precision = 19, scale = 4)
    private BigDecimal pendingQuantity;

    /** Lượng đã rời vị trí nguồn nhưng chưa được xác nhận tại vị trí nhận trong luồng chuyển kho. */
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
