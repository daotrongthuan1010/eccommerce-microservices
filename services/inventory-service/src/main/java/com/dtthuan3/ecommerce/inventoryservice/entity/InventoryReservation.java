package com.dtthuan3.ecommerce.inventoryservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/** Phiếu giữ hàng theo đơn; các dòng và ID liên quan lưu dạng scalar, khóa ngoại do database kiểm soát. */
@Entity
@Table(name = "inventory_reservation",
        uniqueConstraints = @UniqueConstraint(name = "uk_inventory_reservation_operation",
                columnNames = "operation_id"),
        indexes = @Index(name = "idx_inventory_reservation_expiry", columnList = "status, expires_at"))
@Getter
@Setter
@NoArgsConstructor
public class InventoryReservation {
    /** ID reservation ổn định do Order cấp; đồng thời là khóa chính nội bộ. */
    @Id @Column(name = "id", nullable = false, length = 100) private String id;
    /** Mã đơn sở hữu reservation, không ánh xạ quan hệ JPA sang Order. */
    @Column(name = "order_id", nullable = false, length = 100) private String orderId;
    /** Enterprise lấy từ các dòng Inventory, không tin enterprise client tự gán. */
    @Column(name = "enterprise_id", nullable = false, length = 100) private String enterpriseId;
    /** Idempotency key của lệnh tạo reservation. */
    @Column(name = "operation_id", nullable = false, length = 120) private String operationId;
    /** Hash nội dung lệnh để phát hiện dùng lại khóa với dữ liệu khác. */
    @Column(name = "payload_hash", nullable = false, length = 64) private String payloadHash;
    /** HELD, COMMITTED, PARTIALLY_DISPATCHED, RELEASED, EXPIRED, CONSUMED_PARTIAL hoặc CONSUMED. */
    @Column(nullable = false, length = 32) private String status;
    /** Hạn giữ theo UTC; commit chỉ được phép trước thời điểm này theo đồng hồ database. */
    @Column(name = "expires_at", nullable = false) private Instant expiresAt;
    /** Tài khoản tạo lệnh; với lệnh service-to-service là principal từ JWT. */
    @Column(name = "created_by", nullable = false, length = 255) private String createdBy;
    /** Người xác nhận commit; null khi chưa commit. */
    @Column(name = "committed_by", length = 255) private String committedBy;
    /** Mã tham chiếu thanh toán đã được Order xác minh, lưu để phục vụ đối soát. */
    @Column(name = "payment_reference", length = 150) private String paymentReference;
    /** Version dùng để tuần tự hóa cập nhật header. */
    @Version @Column(nullable = false) private Long version;
    /** Thời điểm tạo header. */
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    /** Thời điểm commit nếu đã xác nhận thanh toán. */
    @Column(name = "committed_at") private Instant committedAt;
}
