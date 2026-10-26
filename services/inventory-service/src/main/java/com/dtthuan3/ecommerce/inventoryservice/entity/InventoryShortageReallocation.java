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

/** Ledger từng lần giữ hàng thay thế; cho phép một shortage được bù từ nhiều Inventory. */
@Entity
@Table(name = "inventory_shortage_reallocation",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_shortage_reallocation_operation",
                        columnNames = {"shortage_case_id", "operation_id"}),
                @UniqueConstraint(name = "uk_shortage_reallocation_id", columnNames = "reallocation_id")
        },
        indexes = @Index(name = "idx_shortage_reallocation_case", columnList = "shortage_case_id, created_at"))
@Getter
@Setter
@NoArgsConstructor
public class InventoryShortageReallocation {
    /** UUID bản ghi ledger nội bộ. */
    @Id
    @Column(nullable = false, length = 36)
    private String id;

    /** Hồ sơ shortage được xử lý; String ID thuần, FK do database initializer thêm. */
    @Column(name = "shortage_case_id", nullable = false, length = 36)
    private String shortageCaseId;

    /** Khóa idempotency của lệnh giữ thay thế. */
    @Column(name = "operation_id", nullable = false, length = 100)
    private String operationId;

    /** Mã allocation mới do Order workflow cấp, duy nhất để tránh ghi nhận giữ hai lần. */
    @Column(name = "reallocation_id", nullable = false, length = 100)
    private String reallocationId;

    /** Inventory đích đã giữ hàng thay thế; chỉ lưu ID, database bảo đảm khóa ngoại. */
    @Column(name = "target_inventory_id", nullable = false, length = 36)
    private String targetInventoryId;

    /** Số lượng được giữ ở lần này. */
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal quantity;

    /** Tổng lượng đã tái phân bổ sau thao tác này, dùng để trả idempotent response ổn định. */
    @Column(name = "total_reallocated_after", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalReallocatedAfter;

    /** Lượng thiếu còn lại sau thao tác này. */
    @Column(name = "remaining_shortage_after", nullable = false, precision = 19, scale = 4)
    private BigDecimal remainingShortageAfter;

    /** Trạng thái shortage sau thao tác: OPEN nếu còn thiếu, REPLACEMENT_RESERVED nếu đã đủ. */
    @Column(name = "shortage_status_after", nullable = false, length = 30)
    private String shortageStatusAfter;

    /** Version balance kho thay sau khi giữ để client dễ kiểm tra khi tiếp tục thao tác. */
    @Column(name = "target_inventory_version", nullable = false)
    private Long targetInventoryVersion;

    /** Hash payload để từ chối tái sử dụng operationId với nội dung khác. */
    @Column(name = "payload_hash", nullable = false, length = 64)
    private String payloadHash;

    /** Người khởi chạy việc giữ thay thế. */
    @Column(name = "reserved_by", nullable = false, length = 255)
    private String reservedBy;

    /** Thời điểm ghi ledger theo giờ ứng dụng. */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
