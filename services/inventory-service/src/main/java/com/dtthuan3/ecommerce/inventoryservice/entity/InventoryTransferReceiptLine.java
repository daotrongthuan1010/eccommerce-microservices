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

/** Chi tiết kiểm đếm của một dòng trong một operation nhận phiếu điều chuyển. */
@Entity
@Table(name = "inventory_transfer_receipt_line",
        uniqueConstraints = @UniqueConstraint(name = "uk_transfer_receipt_operation_line",
                columnNames = {"operation_id", "transfer_line_id"}),
        indexes = {
                @Index(name = "idx_transfer_receipt_line_transfer_line", columnList = "transfer_line_id"),
                @Index(name = "idx_transfer_receipt_line_destination_inventory", columnList = "destination_inventory_id")
        })
@Getter
@Setter
@NoArgsConstructor
public class InventoryTransferReceiptLine {
    /** UUID nội bộ của chi tiết nhận. */
    @Id
    @Column(nullable = false, length = 36, updatable = false)
    private String id;

    /** ID operation nhận; database giữ khóa ngoại, entity không ánh xạ quan hệ JPA. */
    @Column(name = "operation_id", nullable = false, length = 36)
    private String operationId;

    /** ID dòng phiếu điều chuyển được kiểm đếm. */
    @Column(name = "transfer_line_id", nullable = false, length = 36)
    private String transferLineId;

    /** SKU được nhận theo dòng phiếu. */
    @Column(nullable = false, length = 100)
    private String sku;

    /** Lượng đạt chuẩn được cộng vào tồn khả dụng ở kho đích. */
    @Column(name = "good_quantity", nullable = false, precision = 19, scale = 4)
    private BigDecimal goodQuantity;

    /** Lượng hỏng được cộng vào tồn vật lý và bucket defective ở kho đích. */
    @Column(name = "damaged_quantity", nullable = false, precision = 19, scale = 4)
    private BigDecimal damagedQuantity;

    /** Lượng khai báo thất lạc, không được cộng vào tồn kho đích. */
    @Column(name = "missing_quantity", nullable = false, precision = 19, scale = 4)
    private BigDecimal missingQuantity;

    /** Biên bản/lý do sai lệch; bắt buộc ở tầng nghiệp vụ nếu missingQuantity lớn hơn 0. */
    @Column(name = "discrepancy_reason", length = 500)
    private String discrepancyReason;

    /** ID tồn nguồn đã giảm in-transit trong lần nhận. */
    @Column(name = "source_inventory_id", nullable = false, length = 36)
    private String sourceInventoryId;

    /** Version tồn nguồn sau khi giảm lượng đã đối soát. */
    @Column(name = "source_inventory_version_after")
    private Long sourceInventoryVersionAfter;

    /** ID tồn đích; null nếu lần nhận chỉ khai báo thất lạc mà không nhận hàng thực tế. */
    @Column(name = "destination_inventory_id", length = 36)
    private String destinationInventoryId;

    /** Version tồn đích sau khi cộng lượng hàng tốt/hỏng. */
    @Column(name = "destination_inventory_version_after")
    private Long destinationInventoryVersionAfter;
}
