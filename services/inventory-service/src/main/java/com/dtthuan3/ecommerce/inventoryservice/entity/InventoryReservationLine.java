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

/** Theo dõi lượng giữ, đã xuất và đã nhả riêng cho từng dòng reservation. */
@Entity
@Table(name = "inventory_reservation_line",
        uniqueConstraints = @UniqueConstraint(name = "uk_inventory_reservation_line_number",
                columnNames = {"reservation_id", "line_number"}),
        indexes = @Index(name = "idx_inventory_reservation_line_inventory", columnList = "inventory_id"))
@Getter
@Setter
@NoArgsConstructor
public class InventoryReservationLine {
    /** ID duy nhất của dòng reservation. */
    @Id @Column(name = "id", nullable = false, length = 36) private String id;
    /** ID header reservation; quan hệ được bảo vệ bằng FK, không có quan hệ JPA. */
    @Column(name = "reservation_id", nullable = false, length = 100) private String reservationId;
    /** Số thứ tự dòng trong lệnh gốc. */
    @Column(name = "line_number", nullable = false) private Integer lineNumber;
    /** Inventory đã được giữ; database bảo đảm ID tồn tại. */
    @Column(name = "inventory_id", nullable = false, length = 36) private String inventoryId;
    /** SKU snapshot để response không cần gọi Product Service. */
    @Column(nullable = false, length = 100) private String sku;
    /** Lượng yêu cầu và được giữ lúc tạo reservation. */
    @Column(name = "requested_quantity", nullable = false, precision = 19, scale = 4)
    private BigDecimal requestedQuantity;
    /** Lượng đã dispatch khỏi kho cho shipment. */
    @Column(name = "dispatched_quantity", nullable = false, precision = 19, scale = 4)
    private BigDecimal dispatchedQuantity;
    /** Lượng đã release; chỉ residual chưa dispatch mới được nhả. */
    @Column(name = "released_quantity", nullable = false, precision = 19, scale = 4)
    private BigDecimal releasedQuantity;
}
