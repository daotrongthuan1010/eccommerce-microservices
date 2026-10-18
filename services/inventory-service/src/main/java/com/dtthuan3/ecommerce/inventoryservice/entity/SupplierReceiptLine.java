package com.dtthuan3.ecommerce.inventoryservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.Check;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Entity mapping để Hibernate sinh bảng dòng phiếu nhập.
 * Service/repository thao tác bằng JDBC; entity này chỉ là metadata cho schema update.
 */
@Entity
@Table(name = "inventory_supplier_receipt_line",
        uniqueConstraints = @UniqueConstraint(name = "uk_supplier_receipt_line_number",
                columnNames = {"receipt_id", "line_number"}))
@Check(name = "ck_supplier_receipt_line_actual_split",
        constraints = "actual_qty >= 0 AND good_qty >= 0 AND damaged_qty >= 0 " +
                "AND inspection_qty >= 0 AND actual_qty = good_qty + damaged_qty + inspection_qty")
@Getter
@Setter
@NoArgsConstructor
public class SupplierReceiptLine {
    /** Khóa chính UUID của dòng phiếu. */
    @Id
    @Column(nullable = false, updatable = false, length = 36)
    private String id;

    /**
     * Chỉ lưu ID phiếu để entity không tạo quan hệ JPA hay proxy không cần thiết.
     * Khóa ngoại receipt_id -> inventory_supplier_receipt.id được bảo đảm ở database.
     */
    @Column(name = "receipt_id", nullable = false, length = 36)
    private String receiptId;

    /** Thứ tự dòng trong phiếu, duy nhất trong cùng receipt. */
    @Column(name = "line_number", nullable = false)
    private int lineNumber;

    /** Mã SKU dự kiến nhập, được xác thực với Product Service trước khi lưu. */
    @Column(name = "sku_id", nullable = false, length = 100)
    private String skuId;

    /** Đơn vị đo của số lượng dự kiến. */
    @Column(nullable = false, length = 32)
    private String uom;

    /** Số lượng dự kiến nhà cung cấp giao; chưa phải số lượng đã nhận vào tồn. */
    @Column(name = "expected_qty", nullable = false, precision = 19, scale = 4)
    private BigDecimal expectedQty;

    /** Tổng lượng đã thực nhận; mặc định 0 cho phiếu mới tạo nhưng chưa kiểm đếm. */
    @Column(name = "actual_qty", nullable = false, precision = 19, scale = 4,
            columnDefinition = "NUMERIC(19,4) NOT NULL DEFAULT 0")
    private BigDecimal actualQty;

    /** Lượng đã kiểm tra đạt chuẩn; chỉ được ghi tồn ở API duyệt/ghi sổ tiếp theo. */
    @Column(name = "good_qty", nullable = false, precision = 19, scale = 4,
            columnDefinition = "NUMERIC(19,4) NOT NULL DEFAULT 0")
    private BigDecimal goodQty;

    /** Lượng đã kết luận hỏng; API này chỉ ghi nhận, chưa tạo movement sang bucket damaged. */
    @Column(name = "damaged_qty", nullable = false, precision = 19, scale = 4,
            columnDefinition = "NUMERIC(19,4) NOT NULL DEFAULT 0")
    private BigDecimal damagedQty;

    /** Lượng đang chờ kiểm tra chất lượng; chưa được tính vào tồn khả dụng. */
    @Column(name = "inspection_qty", nullable = false, precision = 19, scale = 4,
            columnDefinition = "NUMERIC(19,4) NOT NULL DEFAULT 0")
    private BigDecimal inspectionQty;

    /** Mã lô nhà cung cấp nếu đã biết khi tạo phiếu. */
    @Column(name = "lot_code", length = 120)
    private String lotCode;
}
