package com.dtthuan3.ecommerce.inventoryservice.repository.customrepo;

import com.dtthuan3.ecommerce.inventoryservice.repository.InventoryLocationAccessRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Adapter JDBC cho truy vấn kiểm tra Warehouse và Location.
 * Chỉ đọc ba trường Location và ba trường Warehouse cần thiết; không tạo entity JPA,
 * persistence context hoặc truy vấn theo từng ID riêng lẻ.
 */
@Repository
@RequiredArgsConstructor
public class JdbcInventoryLocationAccessRepository implements InventoryLocationAccessRepository {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    /**
     * Đọc mọi Location trong một truy vấn có khóa SHARE đến khi transaction kết thúc.
     * ORDER BY id làm thứ tự lấy khóa xác định, còn IN (:locationIds) mở rộng an toàn thành bind parameters.
     */
    @Override
    public List<LocationSnapshot> findLocationsForShare(Collection<String> locationIds) {
        if (locationIds == null || locationIds.isEmpty()) return List.of();

        return jdbcTemplate.query("""
                        SELECT id, warehouse_id, active
                        FROM inventory_location
                        WHERE id IN (:locationIds)
                        ORDER BY id
                        FOR SHARE
                        """,
                new MapSqlParameterSource("locationIds", locationIds),
                (resultSet, rowNumber) -> new LocationSnapshot(
                        resultSet.getString("id"),
                        resultSet.getString("warehouse_id"),
                        resultSet.getBoolean("active")));
    }

    /**
     * Một truy vấn theo lô và khóa SHARE các kho nội bộ theo thứ tự ID ổn định.
     * Bảng inventory_warehouse dùng cột khóa chính là id; alias thành warehouse_id
     * để ánh xạ thống nhất với WarehouseSnapshot và cột tham chiếu của Location.
     */
    @Override
    public List<WarehouseSnapshot> findWarehousesForShare(Collection<String> warehouseIds) {
        if (warehouseIds == null || warehouseIds.isEmpty()) return List.of();

        return jdbcTemplate.query("""
                        SELECT id AS warehouse_id, enterprise_id, status
                        FROM inventory_warehouse
                        WHERE id IN (:warehouseIds)
                        ORDER BY id
                        FOR SHARE
                        """,
                new MapSqlParameterSource("warehouseIds", warehouseIds),
                (resultSet, rowNumber) -> new WarehouseSnapshot(
                        resultSet.getString("warehouse_id"),
                        resultSet.getString("enterprise_id"),
                        resultSet.getString("status")));
    }

    /**
     * Đọc kho bằng khóa chính id và khóa dòng để trạng thái không đổi giữa kiểm tra và ghi.
     * Alias id thành warehouse_id để giữ nguyên cấu trúc WarehouseSnapshot.
     */
    @Override
    public Optional<WarehouseSnapshot> findWarehouseForShare(String warehouseId) {
        List<WarehouseSnapshot> matches = jdbcTemplate.query("""
                        SELECT id AS warehouse_id, enterprise_id, status
                        FROM inventory_warehouse
                        WHERE id = :warehouseId
                        FOR SHARE
                        """,
                new MapSqlParameterSource("warehouseId", warehouseId),
                (resultSet, rowNumber) -> new WarehouseSnapshot(
                        resultSet.getString("warehouse_id"),
                        resultSet.getString("enterprise_id"),
                        resultSet.getString("status")));
        return matches.stream().findFirst();
    }
}
