package com.dtthuan3.ecommerce.inventoryservice.repository.customrepo;

import com.dtthuan3.ecommerce.inventoryservice.dto.response.InventoryShortageCaseResponse;
import com.dtthuan3.ecommerce.inventoryservice.dto.response.InventoryShortageCaseDetailResponse;
import com.dtthuan3.ecommerce.inventoryservice.dto.response.ShortageReallocationReservationResponse;
import com.dtthuan3.ecommerce.inventoryservice.repository.InventoryShortageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/** JDBC adapter cho shortage case với unique idempotency key do PostgreSQL bảo vệ. */
@Repository
@RequiredArgsConstructor
public class JdbcInventoryShortageRepository implements InventoryShortageRepository {
    private final JdbcTemplate jdbcTemplate;

    /** Tìm hồ sơ thiếu theo doanh nghiệp và trạng thái; phân trang để vận hành không tải toàn bảng. */
    @Override
    public Page<InventoryShortageCaseResponse> search(String enterpriseId, String status, Pageable pageable) {
        String where = status == null ? " WHERE enterprise_id = ?" : " WHERE enterprise_id = ? AND status = ?";
        Object[] filter = status == null ? new Object[]{enterpriseId} : new Object[]{enterpriseId, status};
        List<Object> parameters = new ArrayList<>(Arrays.asList(filter));
        parameters.add(pageable.getPageSize());
        parameters.add(pageable.getOffset());
        List<InventoryShortageCaseResponse> rows = jdbcTemplate.query("""
                SELECT id, operation_id, enterprise_id, warehouse_id, inventory_id, order_id,
                       order_line_id, allocation_id, committed_quantity, dispatched_quantity,
                       shortage_quantity, status, reason, reported_by, created_at
                FROM inventory_shortage_case
                """ + where + " ORDER BY created_at DESC, id DESC LIMIT ? OFFSET ?",
                (rs, row) -> mapCase(rs), parameters.toArray());
        return PageableExecutionUtils.getPage(rows, pageable, () -> {
            Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM inventory_shortage_case" + where,
                    Long.class, filter);
            return total == null ? 0 : total;
        });
    }

    private InventoryShortageCaseResponse mapCase(ResultSet rs) throws SQLException {
        return new InventoryShortageCaseResponse(rs.getString("id"), rs.getString("operation_id"),
                rs.getString("enterprise_id"), rs.getString("warehouse_id"), rs.getString("inventory_id"),
                rs.getString("order_id"), rs.getString("order_line_id"), rs.getString("allocation_id"),
                rs.getBigDecimal("committed_quantity"), rs.getBigDecimal("dispatched_quantity"),
                rs.getBigDecimal("shortage_quantity"), rs.getString("status"), rs.getString("reason"),
                rs.getString("reported_by"), rs.getTimestamp("created_at").toLocalDateTime());
    }

    @Override
    public Optional<InventoryShortageCaseResponse> findByOperation(String enterpriseId, String operationId) {
        List<InventoryShortageCaseResponse> cases = jdbcTemplate.query("""
                SELECT id, operation_id, enterprise_id, warehouse_id, inventory_id, order_id,
                       order_line_id, allocation_id, committed_quantity, dispatched_quantity,
                       shortage_quantity, status, reason, reported_by, created_at
                FROM inventory_shortage_case
                WHERE enterprise_id = ? AND operation_id = ?
                """, (rs, row) -> new InventoryShortageCaseResponse(
                rs.getString("id"), rs.getString("operation_id"), rs.getString("enterprise_id"),
                rs.getString("warehouse_id"), rs.getString("inventory_id"), rs.getString("order_id"),
                rs.getString("order_line_id"), rs.getString("allocation_id"),
                rs.getBigDecimal("committed_quantity"), rs.getBigDecimal("dispatched_quantity"),
                rs.getBigDecimal("shortage_quantity"), rs.getString("status"), rs.getString("reason"),
                rs.getString("reported_by"), rs.getTimestamp("created_at").toLocalDateTime()),
                enterpriseId, operationId);
        return cases.stream().findFirst();
    }

    @Override
    public Optional<String> findPayloadHash(String enterpriseId, String operationId) {
        List<String> hashes = jdbcTemplate.query("""
                SELECT payload_hash FROM inventory_shortage_case
                WHERE enterprise_id = ? AND operation_id = ?
                """, (rs, row) -> rs.getString("payload_hash"), enterpriseId, operationId);
        return hashes.stream().findFirst();
    }

    /** Đọc hồ sơ cùng tổng lượng giữ và lịch sử phân bổ thay thế bằng số truy vấn cố định. */
    @Override
    public Optional<InventoryShortageCaseDetailResponse> findDetail(String caseId) {
        Optional<InventoryShortageCaseResponse> shortageCase = findCaseById(caseId);
        if (shortageCase.isEmpty()) return Optional.empty();

        List<ShortageReallocationReservationResponse> reallocations = jdbcTemplate.query("""
                SELECT shortage_case_id, operation_id, reallocation_id, target_inventory_id,
                       quantity, total_reallocated_after, remaining_shortage_after,
                       shortage_status_after, target_inventory_version
                FROM inventory_shortage_reallocation
                WHERE shortage_case_id = ?
                ORDER BY created_at, id
                """, (rs, row) -> new ShortageReallocationReservationResponse(
                rs.getString("shortage_case_id"), rs.getString("operation_id"),
                rs.getString("reallocation_id"), rs.getString("target_inventory_id"),
                rs.getBigDecimal("quantity"), rs.getBigDecimal("total_reallocated_after"),
                rs.getBigDecimal("remaining_shortage_after"), rs.getString("shortage_status_after"),
                rs.getLong("target_inventory_version")), caseId);
        BigDecimal total = sumReallocatedQuantity(caseId);
        BigDecimal remaining = shortageCase.get().shortageQuantity().subtract(total).max(BigDecimal.ZERO);
        return Optional.of(new InventoryShortageCaseDetailResponse(shortageCase.get(), total, remaining,
                List.copyOf(reallocations)));
    }

    private Optional<InventoryShortageCaseResponse> findCaseById(String caseId) {
        List<InventoryShortageCaseResponse> cases = jdbcTemplate.query("""
                SELECT id, operation_id, enterprise_id, warehouse_id, inventory_id, order_id,
                       order_line_id, allocation_id, committed_quantity, dispatched_quantity,
                       shortage_quantity, status, reason, reported_by, created_at
                FROM inventory_shortage_case WHERE id = ?
                """, (rs, row) -> new InventoryShortageCaseResponse(
                rs.getString("id"), rs.getString("operation_id"), rs.getString("enterprise_id"),
                rs.getString("warehouse_id"), rs.getString("inventory_id"), rs.getString("order_id"),
                rs.getString("order_line_id"), rs.getString("allocation_id"),
                rs.getBigDecimal("committed_quantity"), rs.getBigDecimal("dispatched_quantity"),
                rs.getBigDecimal("shortage_quantity"), rs.getString("status"), rs.getString("reason"),
                rs.getString("reported_by"), rs.getTimestamp("created_at").toLocalDateTime()), caseId);
        return cases.stream().findFirst();
    }

    @Override
    public boolean insert(ShortageCaseRecord record) {
        return jdbcTemplate.update("""
                INSERT INTO inventory_shortage_case (
                    id, enterprise_id, warehouse_id, inventory_id, operation_id, payload_hash,
                    order_id, order_line_id, allocation_id, committed_quantity, dispatched_quantity,
                    shortage_quantity, status, reason, reported_by, inventory_version, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (enterprise_id, operation_id) DO NOTHING
                """, record.id(), record.enterpriseId(), record.warehouseId(), record.inventoryId(),
                record.operationId(), record.payloadHash(), record.orderId(), record.orderLineId(),
                record.allocationId(), record.committedQuantity(), record.dispatchedQuantity(),
                record.shortageQuantity(), record.status(), record.reason(), record.reportedBy(),
                record.inventoryVersion(), record.createdAt()) == 1;
    }

    @Override
    public Optional<ShortageCaseState> lockShortageCase(String caseId) {
        List<ShortageCaseState> rows = jdbcTemplate.query("""
                SELECT id, enterprise_id, inventory_id, shortage_quantity, status
                FROM inventory_shortage_case
                WHERE id = ?
                FOR UPDATE
                """, (rs, row) -> new ShortageCaseState(rs.getString("id"), rs.getString("enterprise_id"),
                rs.getString("inventory_id"), rs.getBigDecimal("shortage_quantity"), rs.getString("status")),
                caseId);
        return rows.stream().findFirst();
    }

    @Override
    public java.math.BigDecimal sumReallocatedQuantity(String caseId) {
        java.math.BigDecimal total = jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(quantity), 0)
                FROM inventory_shortage_reallocation
                WHERE shortage_case_id = ?
                """, java.math.BigDecimal.class, caseId);
        return total == null ? java.math.BigDecimal.ZERO : total;
    }

    @Override
    public Optional<ReallocationRecord> findReallocation(String caseId, String operationId) {
        List<ReallocationRecord> rows = jdbcTemplate.query("""
                SELECT id, shortage_case_id, operation_id, reallocation_id, target_inventory_id,
                       quantity, total_reallocated_after, remaining_shortage_after,
                       shortage_status_after, payload_hash, reserved_by, target_inventory_version, created_at
                FROM inventory_shortage_reallocation
                WHERE shortage_case_id = ? AND operation_id = ?
                """, (rs, row) -> new ReallocationRecord(rs.getString("id"),
                rs.getString("shortage_case_id"), rs.getString("operation_id"), rs.getString("reallocation_id"),
                rs.getString("target_inventory_id"), rs.getBigDecimal("quantity"),
                rs.getBigDecimal("total_reallocated_after"), rs.getBigDecimal("remaining_shortage_after"),
                rs.getString("shortage_status_after"), rs.getString("payload_hash"), rs.getString("reserved_by"),
                rs.getLong("target_inventory_version"), rs.getTimestamp("created_at").toLocalDateTime()),
                caseId, operationId);
        return rows.stream().findFirst();
    }

    @Override
    public boolean insertReallocation(ReallocationRecord record) {
        return jdbcTemplate.update("""
                INSERT INTO inventory_shortage_reallocation (
                    id, shortage_case_id, operation_id, reallocation_id, target_inventory_id, quantity,
                    total_reallocated_after, remaining_shortage_after, shortage_status_after,
                    target_inventory_version, payload_hash, reserved_by, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT DO NOTHING
                """, record.id(), record.shortageCaseId(), record.operationId(), record.reallocationId(),
                record.targetInventoryId(), record.quantity(), record.totalReallocatedAfter(),
                record.remainingShortageAfter(), record.shortageStatusAfter(), record.targetInventoryVersion(),
                record.payloadHash(), record.reservedBy(), record.createdAt()) == 1;
    }

    @Override
    public boolean updateShortageStatus(String caseId, String expectedStatus, String newStatus) {
        return jdbcTemplate.update("""
                UPDATE inventory_shortage_case SET status = ?
                WHERE id = ? AND status = ?
                """, newStatus, caseId, expectedStatus) == 1;
    }
}
