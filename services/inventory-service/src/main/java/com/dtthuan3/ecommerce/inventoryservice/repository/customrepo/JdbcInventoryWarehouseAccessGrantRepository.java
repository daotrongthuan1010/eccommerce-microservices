package com.dtthuan3.ecommerce.inventoryservice.repository.customrepo;

import com.dtthuan3.ecommerce.inventoryservice.repository.InventoryWarehouseAccessGrantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Adapter JDBC cho quyền theo warehouse; unique key trong PostgreSQL phân xử hai lệnh cấp đồng thời. */
@Repository
@RequiredArgsConstructor
public class JdbcInventoryWarehouseAccessGrantRepository implements InventoryWarehouseAccessGrantRepository {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public boolean grant(String warehouseId, String subjectId, String grantedBy, Instant grantedAt) {
        return jdbcTemplate.update("""
                INSERT INTO inventory_warehouse_access_grant
                    (id, warehouse_id, subject_id, granted_by, granted_at)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT (warehouse_id, subject_id) DO NOTHING
                """, UUID.randomUUID().toString(), warehouseId, subjectId, grantedBy,
                Timestamp.from(grantedAt)) == 1;
    }

    @Override
    public boolean revoke(String warehouseId, String subjectId) {
        return jdbcTemplate.update("""
                DELETE FROM inventory_warehouse_access_grant
                WHERE warehouse_id = ? AND subject_id = ?
                """, warehouseId, subjectId) == 1;
    }

    @Override
    public boolean hasAccess(String warehouseId, String subjectId) {
        Boolean exists = jdbcTemplate.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM inventory_warehouse_access_grant
                    WHERE warehouse_id = ? AND subject_id = ?)
                """, Boolean.class, warehouseId, subjectId);
        return Boolean.TRUE.equals(exists);
    }

    @Override
    public List<WarehouseAccessGrantRecord> listByWarehouse(String warehouseId) {
        return jdbcTemplate.query("""
                SELECT warehouse_id, subject_id, granted_by, granted_at
                FROM inventory_warehouse_access_grant
                WHERE warehouse_id = ?
                ORDER BY subject_id
                """, (rs, row) -> new WarehouseAccessGrantRecord(rs.getString("warehouse_id"),
                rs.getString("subject_id"), rs.getString("granted_by"), rs.getTimestamp("granted_at").toInstant()),
                warehouseId);
    }
}
