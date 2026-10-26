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

/** Shortage case phát sinh khi kho không tìm thấy đủ lượng đã cam kết trong bước soạn hàng. */
@Entity
@Table(name = "inventory_shortage_case",
        uniqueConstraints = @UniqueConstraint(name = "uk_inventory_shortage_operation",
                columnNames = {"enterprise_id", "operation_id"}),
        indexes = {
                @Index(name = "idx_inventory_shortage_order_line", columnList = "order_id, order_line_id"),
                @Index(name = "idx_inventory_shortage_status_created", columnList = "status, created_at")
        })
@Getter
@Setter
@NoArgsConstructor
public class InventoryShortageCase {
    /** UUID do Inventory Service sinh cho hồ sơ thiếu hàng. */
    @Id
    @Column(nullable = false, length = 36)
    private String id;

    /** Doanh nghiệp được lấy từ Inventory nguồn, không tin giá trị doanh nghiệp do client tự gửi. */
    @Column(name = "enterprise_id", nullable = false, length = 100)
    private String enterpriseId;

    /** Kho nguồn được lấy từ Inventory bị thiếu. */
    @Column(name = "warehouse_id", nullable = false, length = 36)
    private String warehouseId;

    /** Inventory nguồn; Java chỉ lưu ID, database giữ khóa ngoại. */
    @Column(name = "inventory_id", nullable = false, length = 36)
    private String inventoryId;

    /** Idempotency key do caller cấp để retry không tạo hai shortage case. */
    @Column(name = "operation_id", nullable = false, length = 100)
    private String operationId;

    /** Hash payload để operationId không bị dùng lại cho sự kiện thiếu hàng khác. */
    @Column(name = "payload_hash", nullable = false, length = 64)
    private String payloadHash;

    /** Tham chiếu đơn hàng từ Order Service; không có quan hệ JPA liên service. */
    @Column(name = "order_id", nullable = false, length = 100)
    private String orderId;

    /** Tham chiếu dòng đơn hàng bị ảnh hưởng. */
    @Column(name = "order_line_id", nullable = false, length = 100)
    private String orderLineId;

    /** Allocation cũ để luồng tái phân bổ quản lý phần đã dispatch và phần còn cam kết. */
    @Column(name = "allocation_id", nullable = false, length = 100)
    private String allocationId;

    /** Tổng lượng đã cam kết từ kho nguồn tại lúc phát hiện. */
    @Column(name = "committed_quantity", nullable = false, precision = 19, scale = 4)
    private BigDecimal committedQuantity;

    /** Lượng đã dispatch, không được đưa vào thao tác tái phân bổ. */
    @Column(name = "dispatched_quantity", nullable = false, precision = 19, scale = 4)
    private BigDecimal dispatchedQuantity;

    /** Lượng thiếu do kho xác nhận, thuộc phần chưa dispatch. */
    @Column(name = "shortage_quantity", nullable = false, precision = 19, scale = 4)
    private BigDecimal shortageQuantity;

    /** Hồ sơ mới được tạo ở trạng thái OPEN để Order/CS xử lý bước kế tiếp. */
    @Column(nullable = false, length = 30)
    private String status;

    /** Lý do/ghi chú nghiệp vụ của nhân viên phát hiện thiếu. */
    @Column(nullable = false, length = 500)
    private String reason;

    /** Người báo thiếu hàng. */
    @Column(name = "reported_by", nullable = false, length = 255)
    private String reportedBy;

    /** Version balance nguồn tại lúc xác nhận shortage để phục vụ đối chiếu. */
    @Column(name = "inventory_version", nullable = false)
    private Long inventoryVersion;

    /** Thời điểm nhận báo cáo shortage. */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
