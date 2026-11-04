package com.dtthuan3.ecommerce.inventoryservice.repository.customrepo;

import com.dtthuan3.ecommerce.inventoryservice.repository.InventoryReturnReceiptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Adapter JDBC khóa hạn mức theo dòng trả để chống nhận vượt lượng duyệt khi có request đồng thời.
 * Class/interface này chứa hợp đồng hoặc phần thực thi truy vấn SQL/JDBC tùy chỉnh như khóa dòng, cập nhật có điều kiện và phân trang; repository tập trung thao tác dữ liệu, không điều phối toàn bộ use case.
 */
@Repository
@RequiredArgsConstructor
public class JdbcInventoryReturnReceiptRepository implements InventoryReturnReceiptRepository {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public Optional<ReturnLineProgress> lockProgress(String returnRequestId, String orderLineId) {
        List<ReturnLineProgress> rows = jdbcTemplate.query("""
                SELECT enterprise_id, sku, approved_quantity, received_quantity, source_version, approval_event_id
                FROM inventory_return_line_progress
                WHERE return_request_id = ? AND order_line_id = ?
                FOR UPDATE
                """, (rs, row) -> new ReturnLineProgress(rs.getString("enterprise_id"), rs.getString("sku"),
                rs.getBigDecimal("approved_quantity"), rs.getBigDecimal("received_quantity"),
                (Long) rs.getObject("source_version"), rs.getString("approval_event_id")),
                returnRequestId, orderLineId);
        return rows.stream().findFirst();
    }

    /** Áp dụng projection phê duyệt theo version, khóa dòng để serialize event đến cùng order line. */
    @Override
    public ApprovalApplyResult applyApproval(ApprovedReturnLine approval) {
        jdbcTemplate.update("""
                INSERT INTO inventory_return_line_progress (
                    id, return_request_id, order_line_id, enterprise_id, sku, approved_quantity,
                    received_quantity, source_version, approval_event_id, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, 0, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                ON CONFLICT (return_request_id, order_line_id) DO NOTHING
                """, UUID.randomUUID().toString(), approval.returnRequestId(), approval.orderLineId(),
                approval.enterpriseId(), approval.sku(), approval.approvedQuantity(),
                approval.sourceVersion(), approval.approvalEventId());

        ReturnLineProgress current = lockProgress(approval.returnRequestId(), approval.orderLineId()).orElse(null);
        if (current == null) return ApprovalApplyResult.CONFLICT;

        // Bản ghi legacy từng được khởi tạo từ request chỉ được thay bằng nguồn phê duyệt thật khi chưa nhận hàng.
        if (current.sourceVersion() == null || current.approvalEventId() == null) {
            if (current.receivedQuantity().signum() > 0) return ApprovalApplyResult.CONFLICT;
            updateApproval(approval);
            return ApprovalApplyResult.APPLIED;
        }
        if (approval.sourceVersion() < current.sourceVersion()) return ApprovalApplyResult.STALE;
        if (approval.sourceVersion() == current.sourceVersion()) {
            return sameApproval(current, approval) ? ApprovalApplyResult.DUPLICATE : ApprovalApplyResult.CONFLICT;
        }
        if (!current.enterpriseId().equals(approval.enterpriseId())
                || !current.sku().equals(approval.sku())
                || approval.approvedQuantity().compareTo(current.receivedQuantity()) < 0) {
            return ApprovalApplyResult.CONFLICT;
        }
        updateApproval(approval);
        return ApprovalApplyResult.APPLIED;
    }

    private boolean sameApproval(ReturnLineProgress current, ApprovedReturnLine approval) {
        return current.enterpriseId().equals(approval.enterpriseId())
                && current.sku().equals(approval.sku())
                && current.approvedQuantity().compareTo(approval.approvedQuantity()) == 0;
    }

    private void updateApproval(ApprovedReturnLine approval) {
        jdbcTemplate.update("""
                UPDATE inventory_return_line_progress
                SET enterprise_id = ?, sku = ?, approved_quantity = ?, source_version = ?,
                    approval_event_id = ?, updated_at = CURRENT_TIMESTAMP
                WHERE return_request_id = ? AND order_line_id = ?
                """, approval.enterpriseId(), approval.sku(), approval.approvedQuantity(),
                approval.sourceVersion(), approval.approvalEventId(),
                approval.returnRequestId(), approval.orderLineId());
    }

    @Override
    public Optional<ReturnReceiptRecord> findReceipt(String returnRequestId, String orderLineId,
                                                     String parcelIdentity) {
        List<ReturnReceiptRecord> records = jdbcTemplate.query("""
                SELECT id, return_request_id, order_line_id, parcel_identity, inventory_id,
                       enterprise_id, warehouse_id, location_id, sku, approved_quantity,
                       received_quantity, line_received_after, inventory_version_after,
                       performed_by, reason, created_at
                FROM inventory_return_receipt
                WHERE return_request_id = ? AND order_line_id = ? AND parcel_identity = ?
                """, (resultSet, rowNumber) -> new ReturnReceiptRecord(
                resultSet.getString("id"), resultSet.getString("return_request_id"),
                resultSet.getString("order_line_id"), resultSet.getString("parcel_identity"),
                resultSet.getString("inventory_id"), resultSet.getString("enterprise_id"),
                resultSet.getString("warehouse_id"), resultSet.getString("location_id"),
                resultSet.getString("sku"), resultSet.getBigDecimal("approved_quantity"),
                resultSet.getBigDecimal("received_quantity"), resultSet.getBigDecimal("line_received_after"),
                resultSet.getLong("inventory_version_after"), resultSet.getString("performed_by"),
                resultSet.getString("reason"),
                resultSet.getTimestamp("created_at").toLocalDateTime()),
                returnRequestId, orderLineId, parcelIdentity);
        return records.stream().findFirst();
    }

    /** Đọc chi tiết parcel đã nhận, gồm thông tin vị trí và lượng đã cộng dồn cho dòng trả. */
    @Override
    public Optional<ReturnReceiptRecord> findReceiptById(String receiptId) {
        List<ReturnReceiptRecord> records = jdbcTemplate.query("""
                SELECT id, return_request_id, order_line_id, parcel_identity, inventory_id,
                       enterprise_id, warehouse_id, location_id, sku, approved_quantity,
                       received_quantity, line_received_after, inventory_version_after,
                       performed_by, reason, created_at
                FROM inventory_return_receipt WHERE id = ?
                """, (rs, row) -> new ReturnReceiptRecord(rs.getString("id"),
                rs.getString("return_request_id"), rs.getString("order_line_id"),
                rs.getString("parcel_identity"), rs.getString("inventory_id"),
                rs.getString("enterprise_id"), rs.getString("warehouse_id"),
                rs.getString("location_id"), rs.getString("sku"), rs.getBigDecimal("approved_quantity"),
                rs.getBigDecimal("received_quantity"), rs.getBigDecimal("line_received_after"),
                rs.getLong("inventory_version_after"), rs.getString("performed_by"),
                rs.getString("reason"), rs.getTimestamp("created_at").toLocalDateTime()), receiptId);
        return records.stream().findFirst();
    }

    /**
     * Khóa biên nhận parcel trong transaction hiện tại để hai lượt kiểm tra đồng thời
     * không thể cùng trừ pending quantity hoặc tạo hai kết quả phân loại.
     */
    @Override
    public Optional<ReturnReceiptRecord> findReceiptByIdForUpdate(String receiptId) {
        List<ReturnReceiptRecord> records = jdbcTemplate.query("""
                SELECT id, return_request_id, order_line_id, parcel_identity, inventory_id,
                       enterprise_id, warehouse_id, location_id, sku, approved_quantity,
                       received_quantity, line_received_after, inventory_version_after,
                       performed_by, reason, created_at
                FROM inventory_return_receipt
                WHERE id = ?
                FOR UPDATE
                """, (resultSet, rowNumber) -> new ReturnReceiptRecord(
                resultSet.getString("id"), resultSet.getString("return_request_id"),
                resultSet.getString("order_line_id"), resultSet.getString("parcel_identity"),
                resultSet.getString("inventory_id"), resultSet.getString("enterprise_id"),
                resultSet.getString("warehouse_id"), resultSet.getString("location_id"),
                resultSet.getString("sku"), resultSet.getBigDecimal("approved_quantity"),
                resultSet.getBigDecimal("received_quantity"), resultSet.getBigDecimal("line_received_after"),
                resultSet.getLong("inventory_version_after"), resultSet.getString("performed_by"),
                resultSet.getString("reason"), resultSet.getTimestamp("created_at").toLocalDateTime()),
                receiptId);
        return records.stream().findFirst();
    }

    @Override
    public ReturnReceiptRecord insertReceipt(ReturnReceiptRecord receipt) {
        LocalDateTime createdAt = jdbcTemplate.queryForObject("""
                INSERT INTO inventory_return_receipt (
                    id, return_request_id, order_line_id, parcel_identity, inventory_id,
                    enterprise_id, warehouse_id, location_id, sku, approved_quantity,
                    received_quantity, line_received_after, inventory_version_after,
                    performed_by, reason, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                RETURNING created_at
                """, LocalDateTime.class, receipt.id(), receipt.returnRequestId(), receipt.orderLineId(),
                receipt.parcelIdentity(), receipt.inventoryId(), receipt.enterpriseId(),
                receipt.warehouseId(), receipt.locationId(), receipt.sku(), receipt.approvedQuantity(),
                receipt.receivedQuantity(), receipt.lineReceivedAfter(), receipt.inventoryVersionAfter(),
                receipt.performedBy(), receipt.reason());
        return new ReturnReceiptRecord(receipt.id(), receipt.returnRequestId(), receipt.orderLineId(),
                receipt.parcelIdentity(), receipt.inventoryId(), receipt.enterpriseId(), receipt.warehouseId(),
                receipt.locationId(), receipt.sku(), receipt.approvedQuantity(), receipt.receivedQuantity(),
                receipt.lineReceivedAfter(), receipt.inventoryVersionAfter(), receipt.performedBy(),
                receipt.reason(), createdAt);
    }

    @Override
    public boolean addReceivedQuantity(String returnRequestId, String orderLineId, BigDecimal quantity) {
        return jdbcTemplate.update("""
                UPDATE inventory_return_line_progress
                SET received_quantity = received_quantity + ?, updated_at = CURRENT_TIMESTAMP
                WHERE return_request_id = ? AND order_line_id = ?
                  AND received_quantity + ? <= approved_quantity
                  AND ? > 0
                """, quantity, returnRequestId, orderLineId, quantity, quantity) == 1;
    }
}
