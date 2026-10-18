package com.dtthuan3.ecommerce.inventoryservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity mapping để Hibernate sinh bảng liên kết phiếu nhập với tài liệu.
 * Bảng chỉ lưu asset ID; nội dung và trạng thái tệp do File Service sở hữu.
 */
@Entity
@Table(name = "inventory_supplier_receipt_document",
        uniqueConstraints = @UniqueConstraint(name = "uk_supplier_receipt_document",
                columnNames = {"receipt_id", "asset_id"}))
@Getter
@Setter
@NoArgsConstructor
public class SupplierReceiptDocument {
    /** Khóa chính UUID của liên kết tài liệu. */
    @Id
    @Column(nullable = false, updatable = false, length = 36)
    private String id;

    /** Chỉ lưu ID phiếu; database giữ khóa ngoại để bảo đảm document luôn thuộc một receipt hợp lệ. */
    @Column(name = "receipt_id", nullable = false, length = 36)
    private String receiptId;

    /** ID asset bên File Service; kiểm tra READY sẽ thuộc bước submit phiếu. */
    @Column(name = "asset_id", nullable = false, length = 160)
    private String assetId;
}
