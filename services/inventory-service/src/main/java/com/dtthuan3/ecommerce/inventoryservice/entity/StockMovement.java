package com.dtthuan3.ecommerce.inventoryservice.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Một bản ghi bất biến mô tả tác động của một nghiệp vụ lên tồn kho. */
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "stock_movement", indexes = {
        @Index(name = "idx_movement_inventory_created", columnList = "inventory_id, created_at"),
        @Index(name = "idx_movement_type_created", columnList = "movement_type, created_at"),
        @Index(name = "idx_movement_created", columnList = "created_at"),
        @Index(name = "idx_movement_reference", columnList = "reference_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockMovement {

    @Id
    @Column(name = "id", nullable = false, updatable = false, length = 36)
    private String id;

    /** Inventory chịu tác động; mỗi movement chỉ thuộc về một inventory. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "inventory_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_stock_movement_inventory")
    )
    private Inventory inventory;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 50)
    private MovementType movementType;

    /** Lượng thay đổi; trạng thái trước/sau bên dưới thể hiện chiều thay đổi cụ thể. */
    @Column(name = "quantity", nullable = false, precision = 19, scale = 4)
    private BigDecimal quantity;

    @Column(name = "physical_before", nullable = false, precision = 19, scale = 4)
    private BigDecimal physicalBefore;

    @Column(name = "physical_after", nullable = false, precision = 19, scale = 4)
    private BigDecimal physicalAfter;

    @Column(name = "reserved_before", nullable = false, precision = 19, scale = 4)
    private BigDecimal reservedBefore;

    @Column(name = "reserved_after", nullable = false, precision = 19, scale = 4)
    private BigDecimal reservedAfter;

    /** Thông tin đối soát với đơn hàng, phiếu nhập hoặc mã giao dịch chuyển kho. */
    @Column(name = "reference_type", length = 50)
    private String referenceType;

    @Column(name = "reference_id", length = 100)
    private String referenceId;

    @Column(name = "reason", length = 500)
    private String reason;

    /** Định danh người thao tác; lưu dạng chuỗi để hỗ trợ cả UUID lẫn subject JWT. */
    @Column(name = "performed_by", length = 255)
    private String performedBy;

    @Column(name = "performed_by_subject", length = 255)
    private String performedBySubject;

    /** Thời điểm tạo movement; JPA Auditing gán khi lưu bằng JPA, còn SQL insert dùng giờ DB. */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
