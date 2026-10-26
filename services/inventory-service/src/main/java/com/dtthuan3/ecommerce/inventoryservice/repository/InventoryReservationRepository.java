package com.dtthuan3.ecommerce.inventoryservice.repository;

import com.dtthuan3.ecommerce.inventoryservice.dto.response.InventoryReservationResponse;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Cổng truy cập reservation; service chỉ phụ thuộc hợp đồng, toàn bộ lệnh ghi dùng JDBC. */
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

    record ReservationHeaderRecord(String id, String orderId, String enterpriseId, String operationId,
                                   String payloadHash, String status, Instant expiresAt, long version,
                                   String createdBy, Instant createdAt, String committedBy,
                                   Instant committedAt, String paymentReference) {}
    record ReservationLineRecord(String id, String reservationId, int lineNumber, String inventoryId,
                                 String sku, BigDecimal requestedQuantity, BigDecimal dispatchedQuantity,
                                 BigDecimal releasedQuantity) {}
    record ReservationOperationRecord(String id, String reservationId, String operationId,
                                      String operationType, String payloadHash, String statusAfter,
                                      long versionAfter, String referenceId, Instant createdAt) {}
}
