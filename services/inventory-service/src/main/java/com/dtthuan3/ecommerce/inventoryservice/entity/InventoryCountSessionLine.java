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

import java.math.BigDecimal;

/**
 * Dòng baseline bất biến của một balance tại lúc mở phiên kiểm kê.
 * Entity giữ ID dạng String và không ánh xạ quan hệ JPA; database kiểm soát FK tới phiên và Inventory.
 */
@Entity
@Table(name = "inventory_count_session_line",
        uniqueConstraints = @UniqueConstraint(name = "uk_inventory_count_line_balance",
                columnNames = {"session_id", "inventory_id"}),
        indexes = @Index(name = "idx_inventory_count_line_session", columnList = "session_id, sku"))
@Getter
@Setter
@NoArgsConstructor
public class InventoryCountSessionLine {
    /** UUID của dòng baseline. */
    @Id
    @Column(nullable = false, length = 36)
    private String id;

    /** ID phiên kiểm kê cha; database có FK, Java chỉ giữ ID. */
    @Column(name = "session_id", nullable = false, length = 36)
    private String sessionId;

    /** ID balance được chụp tại thời điểm mở phiên; database có FK, không có quan hệ JPA. */
    @Column(name = "inventory_id", nullable = false, length = 36)
    private String inventoryId;

    /** SKU trong snapshot để truy vấn và xuất danh sách mà không join entity. */
    @Column(nullable = false, length = 100)
    private String sku;

    /** Location tại mốc snapshot để nhân viên biết chính xác vị trí cần đếm. */
    // Nullable để Hibernate có thể nâng cấp bảng đã có snapshot trước khi cột vị trí được bổ sung.
    @Column(name = "location_id", length = 36)
    private String locationId;

    /** Số lượng vật lý tại baseline. */
    @Column(name = "physical_quantity", nullable = false, precision = 19, scale = 4)
    private BigDecimal physicalQuantity;

    /** Lượng đã giữ chỗ tại baseline. */
    @Column(name = "reserved_quantity", nullable = false, precision = 19, scale = 4)
    private BigDecimal reservedQuantity;

    /** Lượng lỗi/hỏng tại baseline. */
    @Column(name = "defective_quantity", nullable = false, precision = 19, scale = 4)
    private BigDecimal defectiveQuantity;

    /** Lượng chờ kiểm tra tại baseline. */
    @Column(name = "pending_quantity", nullable = false, precision = 19, scale = 4)
    private BigDecimal pendingQuantity;

    /** Lượng đang vận chuyển tại baseline. */
    @Column(name = "in_transit_quantity", nullable = false, precision = 19, scale = 4)
    private BigDecimal inTransitQuantity;

    /** Version balance tại baseline để phát hiện movement hoặc thay đổi sau mốc chụp. */
    @Column(name = "inventory_version", nullable = false)
    private Long inventoryVersion;
}
