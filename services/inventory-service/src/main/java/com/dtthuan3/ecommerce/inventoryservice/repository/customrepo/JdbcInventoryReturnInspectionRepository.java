package com.dtthuan3.ecommerce.inventoryservice.repository.customrepo;

import com.dtthuan3.ecommerce.inventoryservice.repository.InventoryReturnInspectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** Adapter JDBC cho bảng kết quả kiểm tra hàng trả; mọi câu lệnh nằm trong transaction service. */
@Repository
@RequiredArgsConstructor
public class JdbcInventoryReturnInspectionRepository implements InventoryReturnInspectionRepository {

    private final JdbcTemplate jdbcTemplate;

    /** Lấy kết quả đã ghi nhận của parcel nếu parcel này đã được kiểm tra trước đó. */
    @Override
    public Optional<InspectionRecord> findByReceiptId(String receiptId) {
        List<InspectionRecord> records = jdbcTemplate.query("""
                SELECT id, receipt_id, operation_id, inventory_id, sku, received_quantity,
                       good_quantity, damaged_quantity, inventory_version_after,
                       inspected_by, reason, inspected_at
                FROM inventory_return_inspection
                WHERE receipt_id = ?
                """, (resultSet, rowNumber) -> new InspectionRecord(
                resultSet.getString("id"), resultSet.getString("receipt_id"),
                resultSet.getString("operation_id"), resultSet.getString("inventory_id"),
                resultSet.getString("sku"), resultSet.getBigDecimal("received_quantity"),
                resultSet.getBigDecimal("good_quantity"), resultSet.getBigDecimal("damaged_quantity"),
                resultSet.getLong("inventory_version_after"), resultSet.getString("inspected_by"),
                resultSet.getString("reason"), resultSet.getTimestamp("inspected_at").toLocalDateTime()),
                receiptId);
        return records.stream().findFirst();
    }

    /** Ghi audit phân loại; database unique constraint bảo đảm một kết quả cho mỗi parcel. */
    @Override
    public InspectionRecord insert(InspectionRecord inspection) {
        LocalDateTime inspectedAt = jdbcTemplate.queryForObject("""
                INSERT INTO inventory_return_inspection (
                    id, receipt_id, operation_id, inventory_id, sku, received_quantity,
                    good_quantity, damaged_quantity, inventory_version_after,
                    inspected_by, reason, inspected_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                RETURNING inspected_at
                """, LocalDateTime.class, inspection.id(), inspection.receiptId(),
                inspection.operationId(), inspection.inventoryId(), inspection.sku(),
                inspection.receivedQuantity(), inspection.goodQuantity(), inspection.damagedQuantity(),
                inspection.inventoryVersionAfter(), inspection.inspectedBy(), inspection.reason());
        return new InspectionRecord(inspection.id(), inspection.receiptId(), inspection.operationId(),
                inspection.inventoryId(), inspection.sku(), inspection.receivedQuantity(),
                inspection.goodQuantity(), inspection.damagedQuantity(), inspection.inventoryVersionAfter(),
                inspection.inspectedBy(), inspection.reason(), inspectedAt);
    }
}
