package com.dtthuan3.ecommerce.inventoryservice.repository;

import com.dtthuan3.ecommerce.inventoryservice.dto.response.InventoryReservationResponse;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Cổng truy cập reservation; service chỉ phụ thuộc hợp đồng, toàn bộ lệnh ghi dùng JDBC.
 * Interface này là ranh giới truy cập dữ liệu cho service, gom các thao tác đọc/ghi cần thiết. Cách thực
 * thi bằng JPA hay JDBC nằm trong repository/adapter, không đặt trong controller.
 */
public interface InventoryReservationRepository {
    Optional<ReservationHeaderRecord> findById(String reservationId);
    Optional<ReservationHeaderRecord> findByIdForUpdate(String reservationId);
    Optional<ReservationHeaderRecord> findByOperationId(String operationId);
    List<ReservationLineRecord> findLines(String reservationId);
    boolean insertHeader(ReservationHeaderRecord header);
    void insertLines(String reservationId, List<ReservationLineRecord> lines);
    Optional<ReservationOperationRecord> findOperation(String reservationId, String operationId);
    boolean insertOperation(ReservationOperationRecord operation);
    boolean commit(String reservationId, long expectedVersion, String orderId, String operationId,
                   String paymentReference, String actor, Instant committedAt);
    /** Chỉ expire khi PostgreSQL xác nhận hạn đã qua và reservation vẫn đang HELD. */
    boolean expireIfDue(String reservationId, long expectedVersion);
    /** Claim batch quá hạn bằng lease; adapter dùng SKIP LOCKED để nhiều instance không cùng lấy một dòng. */
    List<String> claimDueForExpiry(int batchSize, int leaseSeconds);
    boolean updateStatus(String reservationId, long expectedVersion, String expectedStatus,
                         String newStatus);
    boolean addDispatched(String lineId, String reservationId, BigDecimal quantity);
    boolean addReleased(String lineId, String reservationId, BigDecimal quantity);

    default InventoryReservationResponse toResponse(ReservationHeaderRecord header,
                                                    List<ReservationLineRecord> lines) {
        List<InventoryReservationResponse.Line> mapped = lines.stream().map(line ->
                new InventoryReservationResponse.Line(line.id(), line.lineNumber(), line.inventoryId(), line.sku(),
                        line.requestedQuantity(), line.dispatchedQuantity(), line.releasedQuantity(),
                        line.requestedQuantity().subtract(line.dispatchedQuantity()).subtract(line.releasedQuantity())))
                .toList();
        return new InventoryReservationResponse(header.id(), header.orderId(), header.enterpriseId(),
                header.status(), header.expiresAt(), header.version(), header.createdBy(), header.createdAt(),
                header.committedBy(), header.committedAt(), mapped);
    }

    /**
     * Dữ liệu header reservation được đọc/khóa bằng JDBC, không tạo quan hệ JPA tới Order hoặc Inventory.
     * Record này là projection/value object mang đúng các cột cần thiết giữa repository và service. Nó không
     * tạo quan hệ JPA; việc khóa, lưu và cập nhật dữ liệu vẫn được thực hiện bởi repository.
     */
    record ReservationHeaderRecord(String id, String orderId, String enterpriseId, String operationId,
                                   String payloadHash, String status, Instant expiresAt, long version,
                                   String createdBy, Instant createdAt, String committedBy,
                                   Instant committedAt, String paymentReference) {}
    /**
     * Dòng phân bổ số lượng cho một Inventory và tiến độ đã dispatch/release.
     * Record này là projection/value object mang đúng các cột cần thiết giữa repository và service. Nó không
     * tạo quan hệ JPA; việc khóa, lưu và cập nhật dữ liệu vẫn được thực hiện bởi repository.
     */
    record ReservationLineRecord(String id, String reservationId, int lineNumber, String inventoryId,
                                 String sku, BigDecimal requestedQuantity, BigDecimal dispatchedQuantity,
                                 BigDecimal releasedQuantity) {}
    /**
     * Dấu vết idempotency của lệnh reserve/commit/release/dispatch để retry không ghi lặp.
     * Record này là projection/value object mang đúng các cột cần thiết giữa repository và service. Nó không
     * tạo quan hệ JPA; việc khóa, lưu và cập nhật dữ liệu vẫn được thực hiện bởi repository.
     */
    record ReservationOperationRecord(String id, String reservationId, String operationId,
                                      String operationType, String payloadHash, String statusAfter,
                                      long versionAfter, String referenceId, Instant createdAt) {}
}
