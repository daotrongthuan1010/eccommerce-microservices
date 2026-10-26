package com.dtthuan3.ecommerce.inventoryservice.repository.customrepo;

import com.dtthuan3.ecommerce.inventoryservice.repository.InventoryReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Adapter JDBC thực thi khóa dòng, compare-and-set và idempotency cho reservation. */
@Repository
@RequiredArgsConstructor
public class JdbcInventoryReservationRepository implements InventoryReservationRepository {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public Optional<ReservationHeaderRecord> findById(String reservationId) {
        return queryHeader("SELECT * FROM inventory_reservation WHERE id = ?", reservationId);
    }

    @Override
    public Optional<ReservationHeaderRecord> findByIdForUpdate(String reservationId) {
        return queryHeader("SELECT * FROM inventory_reservation WHERE id = ? FOR UPDATE", reservationId);
    }

    @Override
    public Optional<ReservationHeaderRecord> findByOperationId(String operationId) {
        return queryHeader("SELECT * FROM inventory_reservation WHERE operation_id = ?", operationId);
    }

    private Optional<ReservationHeaderRecord> queryHeader(String sql, String key) {
        List<ReservationHeaderRecord> rows = jdbcTemplate.query(sql, (rs, row) -> {
            Timestamp committedAt = rs.getTimestamp("committed_at");
            return new ReservationHeaderRecord(rs.getString("id"), rs.getString("order_id"),
                    rs.getString("enterprise_id"), rs.getString("operation_id"), rs.getString("payload_hash"),
                    rs.getString("status"), rs.getTimestamp("expires_at").toInstant(), rs.getLong("version"),
                    rs.getString("created_by"), rs.getTimestamp("created_at").toInstant(),
                    rs.getString("committed_by"), committedAt == null ? null : committedAt.toInstant(),
                    rs.getString("payment_reference"));
        }, key);
        return rows.stream().findFirst();
    }

    @Override
    public List<ReservationLineRecord> findLines(String reservationId) {
        return jdbcTemplate.query("""
                SELECT id, reservation_id, line_number, inventory_id, sku, requested_quantity,
                       dispatched_quantity, released_quantity
                FROM inventory_reservation_line WHERE reservation_id = ? ORDER BY line_number
                """, (rs, row) -> new ReservationLineRecord(rs.getString("id"),
                rs.getString("reservation_id"), rs.getInt("line_number"), rs.getString("inventory_id"),
                rs.getString("sku"), rs.getBigDecimal("requested_quantity"),
                rs.getBigDecimal("dispatched_quantity"), rs.getBigDecimal("released_quantity")), reservationId);
    }

    @Override
    public boolean insertHeader(ReservationHeaderRecord h) {
        return jdbcTemplate.update("""
                INSERT INTO inventory_reservation (id, order_id, enterprise_id, operation_id, payload_hash,
                    status, expires_at, version, created_by, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, 0, ?, ?)
                ON CONFLICT DO NOTHING
                """, h.id(), h.orderId(), h.enterpriseId(), h.operationId(), h.payloadHash(), h.status(),
                Timestamp.from(h.expiresAt()), h.createdBy(), Timestamp.from(h.createdAt())) == 1;
    }

    @Override
    public void insertLines(String reservationId, List<ReservationLineRecord> lines) {
        jdbcTemplate.batchUpdate("""
                INSERT INTO inventory_reservation_line (id, reservation_id, line_number, inventory_id, sku,
                    requested_quantity, dispatched_quantity, released_quantity)
                VALUES (?, ?, ?, ?, ?, ?, 0, 0)
                """, lines, lines.size(), (ps, line) -> {
            ps.setString(1, line.id()); ps.setString(2, reservationId); ps.setInt(3, line.lineNumber());
            ps.setString(4, line.inventoryId()); ps.setString(5, line.sku());
            ps.setBigDecimal(6, line.requestedQuantity());
        });
    }

    @Override
    public Optional<ReservationOperationRecord> findOperation(String reservationId, String operationId) {
        List<ReservationOperationRecord> rows = jdbcTemplate.query("""
                SELECT id, reservation_id, operation_id, operation_type, payload_hash, status_after,
                       version_after, reference_id, created_at FROM inventory_reservation_operation
                WHERE reservation_id = ? AND operation_id = ?
                """, (rs, row) -> new ReservationOperationRecord(rs.getString("id"),
                rs.getString("reservation_id"), rs.getString("operation_id"), rs.getString("operation_type"),
                rs.getString("payload_hash"), rs.getString("status_after"), rs.getLong("version_after"),
                rs.getString("reference_id"), rs.getTimestamp("created_at").toInstant()), reservationId, operationId);
        return rows.stream().findFirst();
    }

    @Override
    public boolean insertOperation(ReservationOperationRecord op) {
        return jdbcTemplate.update("""
                INSERT INTO inventory_reservation_operation (id, reservation_id, operation_id, operation_type,
                    payload_hash, status_after, version_after, reference_id, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT DO NOTHING
                """, UUID.randomUUID().toString(), op.reservationId(), op.operationId(), op.operationType(),
                op.payloadHash(), op.statusAfter(), op.versionAfter(), op.referenceId(),
                Timestamp.from(op.createdAt())) == 1;
    }

    @Override
    public boolean commit(String id, long expectedVersion, String orderId, String operationId,
                          String paymentReference, String actor, Instant committedAt) {
        return jdbcTemplate.update("""
                UPDATE inventory_reservation SET status = 'COMMITTED', committed_by = ?, committed_at = ?,
                    payment_reference = ?,
                    version = version + 1
                WHERE id = ? AND order_id = ? AND status = 'HELD' AND version = ?
                  AND expires_at > CURRENT_TIMESTAMP
                """, actor, Timestamp.from(committedAt), paymentReference, id, orderId, expectedVersion) == 1;
    }

    @Override
    public boolean expireIfDue(String id, long expectedVersion) {
        return jdbcTemplate.update("""
                UPDATE inventory_reservation SET status = 'EXPIRED', version = version + 1
                WHERE id = ? AND status = 'HELD' AND version = ? AND expires_at <= CURRENT_TIMESTAMP
                """, id, expectedVersion) == 1;
    }

    @Override
    public boolean updateStatus(String id, long expectedVersion, String expectedStatus, String newStatus) {
        return jdbcTemplate.update("""
                UPDATE inventory_reservation SET status = ?, version = version + 1
                WHERE id = ? AND status = ? AND version = ?
                """, newStatus, id, expectedStatus, expectedVersion) == 1;
    }

    @Override
    public boolean addDispatched(String lineId, String reservationId, BigDecimal quantity) {
        return jdbcTemplate.update("""
                UPDATE inventory_reservation_line SET dispatched_quantity = dispatched_quantity + ?
                WHERE id = ? AND reservation_id = ? AND requested_quantity - dispatched_quantity
                    - released_quantity >= ? AND ? > 0
                """, quantity, lineId, reservationId, quantity, quantity) == 1;
    }

    @Override
    public boolean addReleased(String lineId, String reservationId, BigDecimal quantity) {
        return jdbcTemplate.update("""
                UPDATE inventory_reservation_line SET released_quantity = released_quantity + ?
                WHERE id = ? AND reservation_id = ? AND requested_quantity - dispatched_quantity
                    - released_quantity >= ? AND ? > 0
                """, quantity, lineId, reservationId, quantity, quantity) == 1;
    }
}
