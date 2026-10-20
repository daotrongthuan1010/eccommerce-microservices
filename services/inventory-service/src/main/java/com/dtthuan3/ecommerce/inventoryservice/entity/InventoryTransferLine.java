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
import org.hibernate.annotations.ColumnDefault;

import java.math.BigDecimal;

/** Dòng SKU dự kiến điều chuyển; không giữ tồn ở bước tạo phiếu. */
@Entity
@Table(name = "inventory_transfer_line",
        uniqueConstraints = @UniqueConstraint(name = "uk_inventory_transfer_line_number",
                columnNames = {"transfer_id", "line_number"}),
        indexes = @Index(name = "idx_inventory_transfer_line_sku", columnList = "sku"))
@Getter
@Setter
@NoArgsConstructor
public class InventoryTransferLine {
    /** UUID nội bộ của dòng phiếu. */
    @Id
    @Column(nullable = false, length = 36, updatable = false)
    private String id;

    /** ID phiếu cha; khóa ngoại do PostgreSQL bảo đảm, Java không giữ entity cha. */
    @Column(name = "transfer_id", nullable = false, length = 36)
    private String transferId;

    /** Thứ tự dòng theo request, bắt đầu từ 1 và duy nhất trong một phiếu. */
    @Column(name = "line_number", nullable = false)
    private Integer lineNumber;

    /** SKU đã được xác thực còn hoạt động trước khi lưu phiếu. */
    @Column(nullable = false, length = 100)
    private String sku;

    /** Lượng dự kiến chuyển; chưa phải lượng tồn đã xuất khỏi kho nguồn. */
    @Column(name = "planned_quantity", nullable = false, precision = 19, scale = 4)
    private BigDecimal plannedQuantity;

    /** Dòng tồn nguồn đã bị trừ ở bước dispatch; chỉ lưu ID String, database giữ foreign key. */
    @Column(name = "source_inventory_id", length = 36)
    private String sourceInventoryId;

    /** Lượng đã xuất khỏi vị trí nguồn; bằng 0 trước khi phiếu được dispatch. */
    @Column(name = "dispatched_quantity", nullable = false, precision = 19, scale = 4)
    private BigDecimal dispatchedQuantity;

    /** Version của dòng tồn nguồn ngay sau dispatch để audit và đối soát bước tiếp theo. */
    @Column(name = "source_inventory_version_after")
    private Long sourceInventoryVersionAfter;

    /** Tổng lượng tốt đã được kho đích xác nhận qua các lần nhận một phần. */
    @Column(name = "received_good_quantity", nullable = false, precision = 19, scale = 4)
    @ColumnDefault("0.0000")
    private BigDecimal receivedGoodQuantity;

    /** Tổng lượng hỏng đã nhận; phần này vào bucket defective, không thể bán. */
    @Column(name = "received_damaged_quantity", nullable = false, precision = 19, scale = 4)
    @ColumnDefault("0.0000")
    private BigDecimal receivedDamagedQuantity;

    /** Tổng lượng đã xác nhận thất lạc và được ghi trong các bản ghi sai lệch. */
    @Column(name = "missing_quantity", nullable = false, precision = 19, scale = 4)
    @ColumnDefault("0.0000")
    private BigDecimal missingQuantity;
}
