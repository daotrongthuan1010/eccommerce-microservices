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
 * Theo dõi hạn mức được duyệt và tổng lượng đã nhận cho một dòng hàng trả.
 * Một dòng có thể được giao thành nhiều parcel; bản ghi này giúp cộng dồn
 * lượng nhận và chặn tổng số lượng vượt hạn mức. Chỉ lưu ID, không tạo quan hệ JPA.
 */
@Entity
@Table(name = "inventory_return_line_progress",
        uniqueConstraints = @UniqueConstraint(name = "uk_return_progress_request_line",
                columnNames = {"return_request_id", "order_line_id"}),
        indexes = @Index(name = "idx_return_progress_request", columnList = "return_request_id"))
@Getter
@Setter
@NoArgsConstructor
public class InventoryReturnLineProgress {

    /** UUID định danh bản ghi tiến độ nhận của dòng trả hàng. */
    @Id
    @Column(nullable = false, length = 36)
    private String id;

    /** ID yêu cầu trả hàng do service nghiệp vụ đơn hàng cấp; không phải JPA relation. */
    @Column(name = "return_request_id", nullable = false, length = 100)
    private String returnRequestId;

    /** ID dòng hàng trong yêu cầu trả; cùng returnRequestId tạo khóa nghiệp vụ của hạn mức. */
    @Column(name = "order_line_id", nullable = false, length = 100)
    private String orderLineId;

    /** Enterprise của order line được Return Service phê duyệt; nhận parcel phải khớp chính xác giá trị này. */
    @Column(name = "enterprise_id", length = 100)
    private String enterpriseId;

    /** SKU chuẩn của order line được phê duyệt; client không được thay SKU khi gửi parcel về kho. */
    @Column(name = "sku", length = 100)
    private String sku;

    /** Hạn mức được duyệt cho dòng hàng; được lưu làm snapshot để so sánh ở các lần nhận sau. */
    @Column(name = "approved_quantity", nullable = false, precision = 19, scale = 4)
    private BigDecimal approvedQuantity;

    /** Tổng số lượng đã nhận qua các parcel; không được vượt approvedQuantity. */
    @Column(name = "received_quantity", nullable = false, precision = 19, scale = 4)
    private BigDecimal receivedQuantity;

    /** Version tăng đơn điệu do Return Service phát; event cũ không được ghi đè quyết định mới. */
    @Column(name = "source_version")
    private Long sourceVersion;

    /** Event ID nguồn đã phê duyệt hạn mức; null nhận diện dữ liệu legacy chưa được xác minh nguồn. */
    @Column(name = "approval_event_id", length = 200)
    private String approvalEventId;

    /** Thời điểm tạo bản ghi tiến độ nhận. */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /** Thời điểm gần nhất tổng lượng đã nhận được cập nhật. */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
