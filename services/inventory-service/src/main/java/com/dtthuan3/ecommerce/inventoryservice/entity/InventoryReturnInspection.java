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
import java.time.LocalDateTime;

/**
 * Kết quả phân loại chất lượng của một parcel hàng trả đã được nhận vào khu chờ kiểm tra.
 * Một biên nhận chỉ được chốt kiểm tra một lần; entity lưu các ID đơn thuần, không dùng quan hệ JPA.
 */
@Entity
@Table(name = "inventory_return_inspection",
        uniqueConstraints = @UniqueConstraint(name = "uk_return_inspection_receipt", columnNames = "receipt_id"),
        indexes = @Index(name = "idx_return_inspection_inventory", columnList = "inventory_id"))
@Getter
@Setter
@NoArgsConstructor
public class InventoryReturnInspection {

    /** UUID định danh kết quả kiểm tra. */
    @Id
    @Column(nullable = false, length = 36)
    private String id;

    /** ID biên nhận parcel; khóa ngoại được tạo ở database, không ánh xạ @ManyToOne. */
    @Column(name = "receipt_id", nullable = false, length = 36)
    private String receiptId;

    /** Khóa idempotency do client cấp; giữ nguyên cho các lần retry cùng thao tác. */
    @Column(name = "operation_id", nullable = false, length = 120)
    private String operationId;

    /** ID Inventory đã được cập nhật bucket pending/defective. */
    @Column(name = "inventory_id", nullable = false, length = 36)
    private String inventoryId;

    /** Mã SKU được kiểm tra trong parcel. */
    @Column(nullable = false, length = 100)
    private String sku;

    /** Tổng số lượng parcel được kiểm tra; bằng goodQuantity cộng damagedQuantity. */
    @Column(name = "received_quantity", nullable = false, precision = 19, scale = 4)
    private BigDecimal receivedQuantity;

    /** Số lượng đạt chuẩn, được đưa ra khỏi pending và trở thành tồn khả dụng. */
    @Column(name = "good_quantity", nullable = false, precision = 19, scale = 4)
    private BigDecimal goodQuantity;

    /** Số lượng hỏng, được chuyển từ pending sang defective. */
    @Column(name = "damaged_quantity", nullable = false, precision = 19, scale = 4)
    private BigDecimal damagedQuantity;

    /** Version Inventory sau khi áp dụng phân loại để đối soát với số dư. */
    @Column(name = "inventory_version_after", nullable = false)
    private Long inventoryVersionAfter;

    /** Subject hoặc tên người kiểm tra lấy từ ngữ cảnh xác thực. */
    @Column(name = "inspected_by", length = 255)
    private String inspectedBy;

    /** Ghi chú kết quả/điều kiện kiểm tra do nhân viên cung cấp. */
    @Column(length = 500)
    private String reason;

    /** Thời điểm kết quả kiểm tra được ghi nhận theo giờ database. */
    @Column(name = "inspected_at", nullable = false)
    private LocalDateTime inspectedAt;
}
