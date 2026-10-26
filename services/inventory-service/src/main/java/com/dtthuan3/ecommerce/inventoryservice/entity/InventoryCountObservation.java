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
 * Bản ghi bất biến cho một lần quan sát một balance; lần đếm lại tạo quan sát mới, không ghi đè lịch sử.
 * Các ID là String thuần và quan hệ được database bảo đảm bằng khóa ngoại.
 */
@Entity
@Table(name = "inventory_count_observation",
        uniqueConstraints = @UniqueConstraint(name = "uk_inventory_count_observation_line",
                columnNames = {"submission_id", "inventory_id"}),
        indexes = @Index(name = "idx_inventory_count_observation_latest",
                columnList = "session_id, inventory_id, counted_at"))
@Getter
@Setter
@NoArgsConstructor
public class InventoryCountObservation {
    /** UUID của lần quan sát. */
    @Id
    @Column(nullable = false, length = 36)
    private String id;

    /** Lô gửi chứa quan sát này; ID chuỗi, database giữ FK. */
    @Column(name = "submission_id", nullable = false, length = 36)
    private String submissionId;

    /** Phiên kiểm kê để truy vấn theo lô; ID chuỗi, database giữ FK. */
    @Column(name = "session_id", nullable = false, length = 36)
    private String sessionId;

    /** Balance được đếm; ID chuỗi, database giữ FK. */
    @Column(name = "inventory_id", nullable = false, length = 36)
    private String inventoryId;

    /** Số lượng hàng tốt thực tế tại vị trí. */
    @Column(name = "counted_good_quantity", nullable = false, precision = 19, scale = 4)
    private BigDecimal countedGoodQuantity;

    /** Số lượng hàng hỏng thực tế tại vị trí. */
    @Column(name = "counted_defective_quantity", nullable = false, precision = 19, scale = 4)
    private BigDecimal countedDefectiveQuantity;

    /** Số lượng hàng đang chờ phân loại thực tế tại vị trí. */
    @Column(name = "counted_pending_quantity", nullable = false, precision = 19, scale = 4)
    private BigDecimal countedPendingQuantity;

    /** Lý do hoặc ghi chú của lần đếm để phục vụ audit và xử lý recount. */
    @Column(name = "count_reason", nullable = false, length = 500)
    private String countReason;

    /** Mã tham chiếu ảnh/chứng từ đếm; không lưu dữ liệu file trực tiếp trong Inventory. */
    @Column(name = "evidence_reference", length = 500)
    private String evidenceReference;

    /** Người thực hiện lần đếm. */
    @Column(name = "counted_by", nullable = false, length = 255)
    private String countedBy;

    /** Thời điểm ghi nhận lần đếm. */
    @Column(name = "counted_at", nullable = false)
    private LocalDateTime countedAt;
}
