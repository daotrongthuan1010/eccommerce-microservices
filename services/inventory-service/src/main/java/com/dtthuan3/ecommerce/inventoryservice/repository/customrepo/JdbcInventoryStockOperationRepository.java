package com.dtthuan3.ecommerce.inventoryservice.repository.customrepo;

import com.dtthuan3.ecommerce.inventoryservice.repository.InventoryStockOperationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** JDBC adapter dùng INSERT ... ON CONFLICT để claim idempotency an toàn giữa nhiều request/instance. */
@Repository
@RequiredArgsConstructor
public class JdbcInventoryStockOperationRepository implements InventoryStockOperationRepository {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public boolean tryStart(String operationId, String operationType, String inventoryId, String payloadHash,
                           LocalDateTime createdAt) {
        return jdbcTemplate.update("""
                INSERT INTO inventory_stock_operation (
                    id, operation_id, operation_type, inventory_id, payload_hash, response_json, created_at
                ) VALUES (?, ?, ?, ?, ?, NULL, ?)
                ON CONFLICT (operation_id) DO NOTHING
                """, UUID.randomUUID().toString(), operationId, operationType, inventoryId, payloadHash,
                createdAt) == 1;
    }

    @Override
    public Optional<StockOperationRecord> findByOperationId(String operationId) {
        List<StockOperationRecord> records = jdbcTemplate.query("""
                        SELECT operation_type, inventory_id, payload_hash, response_json
                        FROM inventory_stock_operation WHERE operation_id = ?
                        """,
                (resultSet, rowNumber) -> new StockOperationRecord(
                        resultSet.getString("operation_type"), resultSet.getString("inventory_id"),
                        resultSet.getString("payload_hash"), resultSet.getString("response_json")),
                operationId);
        return records.stream().findFirst();
    }

    @Override
    public void complete(String operationId, String inventoryId, String responseJson) {
        int updated = jdbcTemplate.update("""
                UPDATE inventory_stock_operation SET inventory_id = ?, response_json = ?
                WHERE operation_id = ? AND response_json IS NULL
                """, inventoryId, responseJson, operationId);
        if (updated != 1) {
            throw new IllegalStateException("Không thể lưu snapshot cho operation nhập/xuất " + operationId);
        }
    }
}
