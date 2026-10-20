package com.dtthuan3.ecommerce.inventoryservice.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Nhật ký biến động ghi lại tác động của một nghiệp vụ lên một dòng Inventory.
 * Mỗi lần nhập, giữ, xuất, điều chỉnh, chuyển kho hoặc nhận hàng trả tạo movement
 * để có thể truy vết số lượng trước và sau thao tác.
 */
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

    /** UUID của bản ghi lịch sử biến động; không thay đổi sau khi tạo. */
    @Id
    @Column(name = "id", nullable = false, updatable = false, length = 36)
    private String id;

    /** Chỉ lưu ID Inventory để tránh quan hệ JPA; database vẫn có FK kiểm soát inventory_id. */
    /** ID Inventory bị tác động; database giữ khóa ngoại, entity không dùng quan hệ JPA. */
    @Column(name = "inventory_id", nullable = false, length = 255)
    private String inventoryId;

    /** Loại nghiệp vụ gây ra biến động, lưu bằng tên enum để dữ liệu dễ đọc và ổn định. */
    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 50)
    private MovementType movementType;

    /** Lượng thay đổi; trạng thái trước/sau bên dưới thể hiện chiều thay đổi cụ thể. */
    /** Lượng thay đổi của nghiệp vụ; các trường before/after bên dưới cho biết số dư liên quan. */
    @Column(name = "quantity", nullable = false, precision = 19, scale = 4)
    private BigDecimal quantity;

    /** Số lượng vật lý của Inventory ngay trước khi áp dụng movement. */
    @Column(name = "physical_before", nullable = false, precision = 19, scale = 4)
    private BigDecimal physicalBefore;

    /** Số lượng vật lý của Inventory ngay sau khi áp dụng movement. */
    @Column(name = "physical_after", nullable = false, precision = 19, scale = 4)
    private BigDecimal physicalAfter;

    /** Số lượng đang giữ trước khi áp dụng movement. */
    @Column(name = "reserved_before", nullable = false, precision = 19, scale = 4)
    private BigDecimal reservedBefore;

    /** Số lượng đang giữ sau khi áp dụng movement. */
    @Column(name = "reserved_after", nullable = false, precision = 19, scale = 4)
    private BigDecimal reservedAfter;

    /** Thông tin đối soát với đơn hàng, phiếu nhập hoặc mã giao dịch chuyển kho. */
    /** Loại đối tượng tham chiếu, ví dụ đơn hàng, phiếu nhập hoặc giao dịch chuyển kho. */
    @Column(name = "reference_type", length = 50)
    private String referenceType;

    /** ID/mã tham chiếu của nghiệp vụ nguồn; không phải lúc nào cũng là khóa ngoại nội bộ. */
    @Column(name = "reference_id", length = 100)
    private String referenceId;

    /** Lý do vận hành cho thao tác, thường dùng với điều chỉnh hoặc kiểm kê. */
    @Column(name = "reason", length = 500)
    private String reason;

    /** Định danh người thao tác; lưu dạng chuỗi để hỗ trợ cả UUID lẫn subject JWT. */
    /** Tên hoặc ID người thao tác được giữ để phục vụ truy vết. */
    @Column(name = "performed_by", length = 255)
    private String performedBy;

    /** Subject của JWT, nếu có; lưu riêng để giữ định danh ổn định từ hệ thống xác thực. */
    @Column(name = "performed_by_subject", length = 255)
    private String performedBySubject;

    /** Thời điểm tạo movement; JPA Auditing gán khi lưu bằng JPA, còn SQL insert dùng giờ DB. */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
