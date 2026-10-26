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

import java.time.Instant;

/** Dấu idempotency cho commit, release, expire và dispatch của reservation. */
@Entity
@Table(name = "inventory_reservation_operation",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_inventory_reservation_operation_key",
                        columnNames = {"reservation_id", "operation_id"}),
                @UniqueConstraint(name = "uk_inventory_reservation_external_reference",
                        columnNames = {"reservation_id", "operation_type", "reference_id"})
        },
        indexes = @Index(name = "idx_inventory_reservation_operation_type", columnList = "reservation_id, operation_type"))
@Getter
@Setter
@NoArgsConstructor
public class InventoryReservationOperation {
    /** ID nội bộ của operation record. */
    @Id @Column(name = "id", nullable = false, length = 36) private String id;
    /** Reservation bị tác động; database thêm khóa ngoại. */
    @Column(name = "reservation_id", nullable = false, length = 100) private String reservationId;
    /** Khóa retry do caller cấp. */
    @Column(name = "operation_id", nullable = false, length = 120) private String operationId;
    /** Loại lệnh để cùng khóa không bị tái dùng giữa các thao tác khác nhau. */
    @Column(name = "operation_type", nullable = false, length = 24) private String operationType;
    /** Hash request chuẩn hóa để retry chỉ hợp lệ khi payload không đổi. */
    @Column(name = "payload_hash", nullable = false, length = 64) private String payloadHash;
    /** Trạng thái reservation sau khi lệnh đã commit. */
    @Column(name = "status_after", nullable = false, length = 32) private String statusAfter;
    /** Version reservation sau lần xử lý đầu. */
    @Column(name = "version_after", nullable = false) private Long versionAfter;
    /** Tham chiếu payment/shipment/reason liên quan đến operation nếu có. */
    @Column(name = "reference_id", length = 150) private String referenceId;
    /** Thời điểm ghi dấu idempotency. */
    @Column(name = "created_at", nullable = false) private Instant createdAt;
}
