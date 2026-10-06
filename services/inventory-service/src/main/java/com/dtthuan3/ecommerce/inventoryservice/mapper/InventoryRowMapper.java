package com.dtthuan3.ecommerce.inventoryservice.mapper;

import com.dtthuan3.ecommerce.inventoryservice.entity.Inventory;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;

/** Chuyển một dòng kết quả JDBC (tên cột snake_case) thành entity Inventory. */
@Component
public class InventoryRowMapper implements RowMapper<Inventory> {

    @Override
    public Inventory mapRow(ResultSet rs, int rowNum)
            throws SQLException {

        return Inventory.builder()
                .id(rs.getString("id"))
                .enterpriseId(rs.getString("enterprise_id"))
                .warehouseId(rs.getString("warehouse_id"))
                .locationId(rs.getString("location_id"))
                .sku(rs.getString("sku"))
                .physicalQuantity(rs.getBigDecimal("physical_quantity"))
                .reservedQuantity(rs.getBigDecimal("reserved_quantity"))
                .defectiveQuantity(rs.getBigDecimal("defective_quantity"))
                .pendingQuantity(rs.getBigDecimal("pending_quantity"))
                .inTransitQuantity(rs.getBigDecimal("in_transit_quantity"))
                .version(rs.getLong("version"))
                .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
                .updatedAt(rs.getTimestamp("updated_at").toLocalDateTime())
                .build();
    }
}
