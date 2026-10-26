package com.dtthuan3.ecommerce.inventoryservice.repository.customrepo;

import com.dtthuan3.ecommerce.inventoryservice.repository.InventoryTransferRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

/** Adapter JDBC của phiếu điều chuyển; dùng SQL trực tiếp cho thao tác nghiệp vụ và batch dòng hàng. */
@Repository
@RequiredArgsConstructor
public class JdbcInventoryTransferRepository implements InventoryTransferRepository {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public Optional<TransferHeaderRecord> findByOperationId(String operationId) {
        return queryHeader("""
                SELECT id, enterprise_id, source_warehouse_id, source_location_id,
                       destination_warehouse_id, destination_location_id, operation_id,
                       payload_hash, reference_code, reason, status, version, created_by, created_at,
                       dispatch_operation_id, dispatch_expected_version, dispatched_by, dispatched_at
                FROM inventory_transfer WHERE operation_id = ?
                """, operationId);
    }

    @Override
    public Optional<TransferHeaderRecord> findById(String transferId) {
        return queryHeader("""
                SELECT id, enterprise_id, source_warehouse_id, source_location_id,
                       destination_warehouse_id, destination_location_id, operation_id,
                       payload_hash, reference_code, reason, status, version, created_by, created_at,
                       dispatch_operation_id, dispatch_expected_version, dispatched_by, dispatched_at
                FROM inventory_transfer WHERE id = ?
                """, transferId);
    }

    @Override
    public Page<TransferHeaderRecord> search(String enterpriseId, String status, Pageable pageable) {
        StringBuilder where = new StringBuilder(" WHERE enterprise_id = ?");
        List<Object> args = new ArrayList<>();
        args.add(enterpriseId);
        if (status != null && !status.isBlank()) {
            where.append(" AND status = ?");
            args.add(status);
        }
        String columns = "id, enterprise_id, source_warehouse_id, source_location_id, destination_warehouse_id, "
                + "destination_location_id, operation_id, payload_hash, reference_code, reason, status, version, "
                + "created_by, created_at, dispatch_operation_id, dispatch_expected_version, dispatched_by, dispatched_at";
        List<TransferHeaderRecord> content = jdbcTemplate.query("SELECT " + columns + " FROM inventory_transfer"
                        + where + " ORDER BY created_at DESC, id DESC LIMIT ? OFFSET ?",
                (rs, row) -> mapHeader(rs), append(args, pageable.getPageSize(), pageable.getOffset()));
        return PageableExecutionUtils.getPage(content, pageable, () -> {
            Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM inventory_transfer" + where,
                    Long.class, args.toArray());
            return count == null ? 0L : count;
        });
    }

    private Object[] append(List<Object> args, Object... tail) {
        List<Object> all = new ArrayList<>(args);
        Collections.addAll(all, tail);
        return all.toArray();
    }

    private TransferHeaderRecord mapHeader(ResultSet rs) throws SQLException {
        Timestamp dispatchedTimestamp = rs.getTimestamp("dispatched_at");
        return new TransferHeaderRecord(rs.getString("id"), rs.getString("enterprise_id"),
                rs.getString("source_warehouse_id"), rs.getString("source_location_id"),
                rs.getString("destination_warehouse_id"), rs.getString("destination_location_id"),
                rs.getString("operation_id"), rs.getString("payload_hash"), rs.getString("reference_code"),
                rs.getString("reason"), rs.getString("status"), rs.getLong("version"),
                rs.getString("created_by"), rs.getTimestamp("created_at").toInstant(),
                rs.getString("dispatch_operation_id"), rs.getObject("dispatch_expected_version", Long.class),
                rs.getString("dispatched_by"), dispatchedTimestamp == null ? null : dispatchedTimestamp.toInstant());
    }

    @Override
    public Optional<TransferHeaderRecord> findByIdForUpdate(String transferId) {
        return queryHeader("""
                SELECT id, enterprise_id, source_warehouse_id, source_location_id,
                       destination_warehouse_id, destination_location_id, operation_id,
                       payload_hash, reference_code, reason, status, version, created_by, created_at,
                       dispatch_operation_id, dispatch_expected_version, dispatched_by, dispatched_at
                FROM inventory_transfer WHERE id = ? FOR UPDATE
                """, transferId);
    }

    /** Ánh xạ chung cho truy vấn header thường và truy vấn đã khóa. */
    private Optional<TransferHeaderRecord> queryHeader(String sql, String key) {
        List<TransferHeaderRecord> records = jdbcTemplate.query(sql, (resultSet, rowNumber) -> {
            Timestamp dispatchedTimestamp = resultSet.getTimestamp("dispatched_at");
            return new TransferHeaderRecord(
                    resultSet.getString("id"), resultSet.getString("enterprise_id"),
                    resultSet.getString("source_warehouse_id"), resultSet.getString("source_location_id"),
                    resultSet.getString("destination_warehouse_id"), resultSet.getString("destination_location_id"),
                    resultSet.getString("operation_id"), resultSet.getString("payload_hash"),
                    resultSet.getString("reference_code"), resultSet.getString("reason"),
                    resultSet.getString("status"), resultSet.getLong("version"),
                    resultSet.getString("created_by"), resultSet.getTimestamp("created_at").toInstant(),
                    resultSet.getString("dispatch_operation_id"),
                    resultSet.getObject("dispatch_expected_version", Long.class),
                    resultSet.getString("dispatched_by"),
                    dispatchedTimestamp == null ? null : dispatchedTimestamp.toInstant());
        }, key);
        return records.stream().findFirst();
    }

    @Override
    public boolean insertHeaderIfAbsent(TransferHeaderRecord header) {
        PreparedStatementCreator insert = connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO inventory_transfer (
                        id, enterprise_id, source_warehouse_id, source_location_id,
                        destination_warehouse_id, destination_location_id, operation_id,
                        payload_hash, reference_code, reason, status, version, created_by, created_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT (operation_id) DO NOTHING
                    """, Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, header.id());
            statement.setString(2, header.enterpriseId());
            statement.setString(3, header.sourceWarehouseId());
            statement.setString(4, header.sourceLocationId());
            statement.setString(5, header.destinationWarehouseId());
            statement.setString(6, header.destinationLocationId());
            statement.setString(7, header.operationId());
            statement.setString(8, header.payloadHash());
            statement.setString(9, header.referenceCode());
            statement.setString(10, header.reason());
            statement.setString(11, header.status());
            statement.setLong(12, header.version());
            statement.setString(13, header.createdBy());
            statement.setTimestamp(14, Timestamp.from(header.createdAt()));
            return statement;
        };
        return jdbcTemplate.update(insert) == 1;
    }

    @Override
    public void insertLines(String transferId, List<TransferLineRecord> lines) {
        jdbcTemplate.batchUpdate("""
                INSERT INTO inventory_transfer_line (
                    id, transfer_id, line_number, sku, planned_quantity,
                    source_inventory_id, dispatched_quantity, source_inventory_version_after,
                    received_good_quantity, received_damaged_quantity, missing_quantity
                ) VALUES (?, ?, ?, ?, ?, NULL, 0, NULL, 0, 0, 0)
                """, lines, lines.size(), (statement, line) -> {
            statement.setString(1, line.lineId());
            statement.setString(2, transferId);
            statement.setInt(3, line.lineNumber());
            statement.setString(4, line.sku());
            statement.setBigDecimal(5, line.plannedQuantity());
        });
    }

    @Override
    public List<TransferLineRecord> findLines(String transferId) {
        return jdbcTemplate.query("""
                SELECT id, line_number, sku, planned_quantity,
                       source_inventory_id, dispatched_quantity, source_inventory_version_after,
                       received_good_quantity, received_damaged_quantity, missing_quantity
                FROM inventory_transfer_line
                WHERE transfer_id = ?
                ORDER BY line_number
                """, (resultSet, rowNumber) -> new TransferLineRecord(
                resultSet.getString("id"), resultSet.getInt("line_number"), resultSet.getString("sku"),
                resultSet.getBigDecimal("planned_quantity"), resultSet.getString("source_inventory_id"),
                resultSet.getBigDecimal("dispatched_quantity"),
                resultSet.getObject("source_inventory_version_after", Long.class),
                resultSet.getBigDecimal("received_good_quantity"),
                resultSet.getBigDecimal("received_damaged_quantity"),
                resultSet.getBigDecimal("missing_quantity")), transferId);
    }

    @Override
    public boolean markLinesDispatched(String transferId, List<TransferLineRecord> lines) {
        int[][] results = jdbcTemplate.batchUpdate("""
                UPDATE inventory_transfer_line
                SET source_inventory_id = ?, dispatched_quantity = ?, source_inventory_version_after = ?
                WHERE id = ? AND transfer_id = ? AND dispatched_quantity = 0
                """, lines, lines.size(), (statement, line) -> {
            statement.setString(1, line.sourceInventoryId());
            statement.setBigDecimal(2, line.dispatchedQuantity());
            if (line.sourceInventoryVersionAfter() == null) {
                statement.setNull(3, Types.BIGINT);
            } else {
                statement.setLong(3, line.sourceInventoryVersionAfter());
            }
            statement.setString(4, line.lineId());
            statement.setString(5, transferId);
        });
        IntStream flattened = Arrays.stream(results)
                .flatMapToInt(Arrays::stream);
        int[] counts = flattened.toArray();
        return counts.length == lines.size() && Arrays.stream(counts)
                .allMatch(count -> count == 1 || count == Statement.SUCCESS_NO_INFO);
    }

    @Override
    public boolean markDispatched(String transferId, long expectedVersion, String operationId,
                                  String dispatchedBy, Instant dispatchedAt) {
        return jdbcTemplate.update("""
                UPDATE inventory_transfer
                SET status = 'IN_TRANSIT', version = version + 1,
                    dispatch_operation_id = ?, dispatch_expected_version = ?,
                    dispatched_by = ?, dispatched_at = ?
                WHERE id = ? AND status IN ('CREATED', 'APPROVED') AND version = ?
                """, operationId, expectedVersion, dispatchedBy, Timestamp.from(dispatchedAt),
                transferId, expectedVersion) == 1;
    }

    /** Duyệt có điều kiện; một trong các thao tác approve/dispatch chỉ có thể thắng trên header. */
    @Override
    public boolean approve(String transferId, long expectedVersion, String operationId, String payloadHash,
                           String approvedBy, Instant approvedAt) {
        return jdbcTemplate.update("""
                UPDATE inventory_transfer
                SET status = 'APPROVED', version = version + 1,
                    approval_operation_id = ?, approval_expected_version = ?,
                    approval_payload_hash = ?, approved_by = ?, approved_at = ?
                WHERE id = ? AND status = 'CREATED' AND version = ?
                """, operationId, expectedVersion, payloadHash, approvedBy,
                Timestamp.from(approvedAt), transferId, expectedVersion) == 1;
    }

    /** Lấy khóa duyệt để so payload khi caller retry. */
    @Override
    public Optional<ApprovalRecord> findApproval(String transferId) {
        List<ApprovalRecord> records = jdbcTemplate.query("""
                SELECT approval_operation_id, approval_expected_version, approval_payload_hash,
                       approved_by, approved_at
                FROM inventory_transfer WHERE id = ? AND approval_operation_id IS NOT NULL
                """, (rs, row) -> {
            Timestamp approvedAt = rs.getTimestamp("approved_at");
            return new ApprovalRecord(rs.getString("approval_operation_id"),
                    rs.getObject("approval_expected_version", Long.class),
                    rs.getString("approval_payload_hash"), rs.getString("approved_by"),
                    approvedAt == null ? null : approvedAt.toInstant());
        }, transferId);
        return records.stream().findFirst();
    }

    /** Cộng dồn tiến độ nhận trong SQL để không thể xác nhận vượt lượng đã dispatch. */
    @Override
    public boolean markLinesReceived(String transferId, List<ReceiptProgressUpdate> updates) {
        if (updates == null || updates.isEmpty()) return true;
        int[][] results = jdbcTemplate.batchUpdate("""
                        UPDATE inventory_transfer_line
                        SET received_good_quantity = ?, received_damaged_quantity = ?, missing_quantity = ?
                        WHERE id = ? AND transfer_id = ?
                          AND dispatched_quantity - received_good_quantity
                              - received_damaged_quantity - missing_quantity >= ?
                          AND ? > 0
                        """, updates, updates.size(), (statement, update) -> {
            statement.setBigDecimal(1, update.receivedGoodQuantity());
            statement.setBigDecimal(2, update.receivedDamagedQuantity());
            statement.setBigDecimal(3, update.missingQuantity());
            statement.setString(4, update.lineId());
            statement.setString(5, transferId);
            statement.setBigDecimal(6, update.accountedQuantity());
            statement.setBigDecimal(7, update.accountedQuantity());
        });
        int[] counts = Arrays.stream(results).flatMapToInt(Arrays::stream).toArray();
        return counts.length == updates.size() && Arrays.stream(counts)
                .allMatch(count -> count == 1 || count == Statement.SUCCESS_NO_INFO);
    }

    /** Chuyển trạng thái phiếu bằng compare-and-set theo version đang được khóa. */
    @Override
    public boolean markReceived(String transferId, long expectedVersion, String statusAfter) {
        return jdbcTemplate.update("""
                        UPDATE inventory_transfer
                        SET status = ?, version = version + 1
                        WHERE id = ? AND version = ?
                          AND status IN ('IN_TRANSIT', 'PARTIALLY_RECEIVED')
                        """, statusAfter, transferId, expectedVersion) == 1;
    }

    @Override
    public Optional<TransferReceiptOperationRecord> findReceiptOperation(String transferId,
                                                                         String operationId) {
        List<TransferReceiptOperationRecord> records = jdbcTemplate.query("""
                        SELECT id, transfer_id, operation_id, payload_hash, status_after,
                               transfer_version_after, received_by, received_at
                        FROM inventory_transfer_receipt_operation
                        WHERE transfer_id = ? AND operation_id = ?
                        """, (resultSet, rowNumber) -> new TransferReceiptOperationRecord(
                        resultSet.getString("id"), resultSet.getString("transfer_id"),
                        resultSet.getString("operation_id"), resultSet.getString("payload_hash"),
                        resultSet.getString("status_after"),
                        resultSet.getObject("transfer_version_after", Long.class),
                        resultSet.getString("received_by"),
                        resultSet.getTimestamp("received_at").toInstant()), transferId, operationId);
        return records.stream().findFirst();
    }

    /** Ghi snapshot operation và lấy timestamp do database sinh để response retry luôn ổn định. */
    @Override
    public TransferReceiptOperationRecord insertReceiptOperation(TransferReceiptOperationRecord operation) {
        Timestamp receivedAt = jdbcTemplate.queryForObject("""
                        INSERT INTO inventory_transfer_receipt_operation (
                            id, transfer_id, operation_id, payload_hash, status_after,
                            transfer_version_after, received_by, received_at
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, clock_timestamp())
                        RETURNING received_at
                        """, Timestamp.class, operation.id(), operation.transferId(), operation.operationId(),
                operation.payloadHash(), operation.statusAfter(), operation.transferVersionAfter(),
                operation.receivedBy());
        return new TransferReceiptOperationRecord(operation.id(), operation.transferId(), operation.operationId(),
                operation.payloadHash(), operation.statusAfter(), operation.transferVersionAfter(),
                operation.receivedBy(), receivedAt.toInstant());
    }

    /** Ghi kiểm đếm từng dòng theo batch trong cùng transaction với các balance. */
    @Override
    public void insertReceiptOperationLines(List<TransferReceiptOperationLineRecord> lines) {
        if (lines == null || lines.isEmpty()) return;
        jdbcTemplate.batchUpdate("""
                        INSERT INTO inventory_transfer_receipt_line (
                            id, operation_id, transfer_line_id, sku, good_quantity, damaged_quantity,
                            missing_quantity, discrepancy_reason, source_inventory_id,
                            source_inventory_version_after, destination_inventory_id,
                            destination_inventory_version_after
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """, lines, lines.size(), (statement, line) -> {
            statement.setString(1, line.id());
            statement.setString(2, line.operationId());
            statement.setString(3, line.transferLineId());
            statement.setString(4, line.sku());
            statement.setBigDecimal(5, line.goodQuantity());
            statement.setBigDecimal(6, line.damagedQuantity());
            statement.setBigDecimal(7, line.missingQuantity());
            statement.setString(8, line.discrepancyReason());
            statement.setString(9, line.sourceInventoryId());
            if (line.sourceInventoryVersionAfter() == null) {
                statement.setNull(10, Types.BIGINT);
            } else {
                statement.setLong(10, line.sourceInventoryVersionAfter());
            }
            statement.setString(11, line.destinationInventoryId());
            if (line.destinationInventoryVersionAfter() == null) {
                statement.setNull(12, Types.BIGINT);
            } else {
                statement.setLong(12, line.destinationInventoryVersionAfter());
            }
        });
    }

    @Override
    public List<TransferReceiptOperationLineRecord> findReceiptOperationLines(String operationId) {
        return jdbcTemplate.query("""
                        SELECT id, operation_id, transfer_line_id, sku, good_quantity, damaged_quantity,
                               missing_quantity, discrepancy_reason, source_inventory_id,
                               source_inventory_version_after, destination_inventory_id,
                               destination_inventory_version_after
                        FROM inventory_transfer_receipt_line
                        WHERE operation_id = ?
                        ORDER BY transfer_line_id
                        """, (resultSet, rowNumber) -> new TransferReceiptOperationLineRecord(
                        resultSet.getString("id"), resultSet.getString("operation_id"),
                        resultSet.getString("transfer_line_id"), resultSet.getString("sku"),
                        resultSet.getBigDecimal("good_quantity"), resultSet.getBigDecimal("damaged_quantity"),
                        resultSet.getBigDecimal("missing_quantity"), resultSet.getString("discrepancy_reason"),
                        resultSet.getString("source_inventory_id"),
                        resultSet.getObject("source_inventory_version_after", Long.class),
                        resultSet.getString("destination_inventory_id"),
                        resultSet.getObject("destination_inventory_version_after", Long.class)), operationId);
    }
}
