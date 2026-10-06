package com.dtthuan3.ecommerce.inventoryservice.repository.customrepo;

import com.dtthuan3.ecommerce.inventoryservice.entity.Inventory;
import com.dtthuan3.ecommerce.inventoryservice.mapper.InventoryRowMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Các truy vấn inventory dùng SQL trực tiếp để cập nhật có điều kiện và khóa dữ liệu khi cần.
 * Vì JdbcTemplate không kích hoạt JPA Auditing, các câu INSERT/UPDATE ở đây tự đặt created_at/updated_at.
 */
@Repository
@RequiredArgsConstructor
public class InventoryRepositoryCustomImpl
        implements InventoryRepositoryCustom {

    private final JdbcTemplate jdbcTemplate;
    private final InventoryRowMapper inventoryMapper;

    /** Đọc một inventory theo ID mà không khóa dòng; dùng cho các truy vấn chỉ đọc. */
    @Override
    public Optional<Inventory> findInventoryRowById(String id) {


        String sql = """
                SELECT
                    id,
                    enterprise_id,
                    warehouse_id,
                    location_id,
                    sku,
                    physical_quantity,
                    reserved_quantity,
                    defective_quantity,
                    pending_quantity,
                    in_transit_quantity,
                    version,
                    created_at,
                    updated_at
                FROM inventory
                WHERE id = ?
                """;

        return queryOne(sql, id);
    }

    /** Đọc và khóa dòng đến hết giao dịch để các thao tác ghi cùng vị trí tồn được tuần tự hóa. */
    @Override
    public Optional<Inventory> findInventoryRowByIdForUpdate(String id) {
        String sql = """
                SELECT id, enterprise_id, warehouse_id, location_id, sku,
                       physical_quantity, reserved_quantity, defective_quantity,
                       pending_quantity, in_transit_quantity, version, created_at, updated_at
                FROM inventory WHERE id = ? FOR UPDATE
                """;
        return queryOne(sql, id);
    }

    /** Tìm các vị trí có SKU này ở mọi kho. */
    @Override
    public List<Inventory> findBySku(String sku) {
        return queryInventories("WHERE sku = ? ORDER BY warehouse_id, location_id", sku);
    }

    /** Tìm toàn bộ vị trí hàng thuộc một kho. */
    @Override
    public List<Inventory> findByWarehouse(String warehouseId) {
        return queryInventories("WHERE warehouse_id = ? ORDER BY location_id, sku", warehouseId);
    }

    /** Tìm toàn bộ vị trí tồn trong một khu vực lưu trữ. */
    @Override
    public List<Inventory> findByLocation(String locationId) {
        return queryInventories("WHERE location_id = ? ORDER BY warehouse_id, sku", locationId);
    }

    /** Chỉ trả hàng còn bán được: tồn vật lý trừ hàng giữ, hàng lỗi và hàng đang chờ. */
    @Override
    public List<Inventory> findAvailable(String sku, String warehouseId, String locationId) {
        // Tham số tùy chọn được thêm vào SQL bằng placeholder, không ghép giá trị trực tiếp vào câu truy vấn.
        StringBuilder where = new StringBuilder("WHERE physical_quantity - reserved_quantity - defective_quantity - pending_quantity > 0");
        List<Object> args = new ArrayList<>();
        if (sku != null) { where.append(" AND sku = ?"); args.add(sku); }
        if (warehouseId != null) { where.append(" AND warehouse_id = ?"); args.add(warehouseId); }
        if (locationId != null) { where.append(" AND location_id = ?"); args.add(locationId); }
        where.append(" ORDER BY warehouse_id, location_id, sku");
        return queryInventories(where.toString(), args.toArray());
    }

    /** Dùng chung phần SELECT và mapper để các truy vấn danh sách có cùng cấu trúc dữ liệu trả về. */
    private List<Inventory> queryInventories(String where, Object... args) {
        String sql = """
                SELECT id, enterprise_id, warehouse_id, location_id, sku,
                       physical_quantity, reserved_quantity, defective_quantity,
                       pending_quantity, in_transit_quantity, version, created_at, updated_at
                FROM inventory
                """ + where;
        return jdbcTemplate.query(sql, inventoryMapper, args);
    }

    /** Tìm duy nhất vị trí tồn theo doanh nghiệp, kho, khu vực lưu trữ và SKU. */
    public Optional<Inventory> findByPosition(
            String enterpriseId,
            String warehouseId,
            String locationId,
            String sku
    ) {

        String sql = """
            SELECT
                id,
                enterprise_id,
                warehouse_id,
                location_id,
                sku,
                physical_quantity,
                reserved_quantity,
                defective_quantity,
                pending_quantity,
                in_transit_quantity,
                version,
                created_at,
                updated_at
            FROM inventory
            WHERE enterprise_id = ?
              AND warehouse_id = ?
              AND location_id = ?
              AND sku = ?
            """;

        return jdbcTemplate
                .query(
                        sql,
                        inventoryMapper,
                        enterpriseId,
                        warehouseId,
                        locationId,
                        sku
                )
                .stream()
                .findFirst();
    }

    /** Tạo vị trí tồn bằng một INSERT; xung đột khóa duy nhất được báo qua Optional rỗng. */
    @Override
    public Optional<Inventory> create(
            String enterpriseId,
            String warehouseId,
            String locationId,
            String sku
    ) {
        return insertInventory(enterpriseId, warehouseId, locationId, sku,
                " ON CONFLICT (enterprise_id, warehouse_id, location_id, sku) DO NOTHING ");
    }

    /** Chèn một lô inventory bằng một JDBC batch; mỗi ID đã được service tạo trước khi gọi. */
    @Override
    public int[] createBatch(List<Inventory> inventories) {
        String sql = """
                INSERT INTO inventory (
                    id, enterprise_id, warehouse_id, location_id, sku,
                    physical_quantity, reserved_quantity, defective_quantity,
                    pending_quantity, in_transit_quantity, version, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, 0, 0, 0, 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                ON CONFLICT (enterprise_id, warehouse_id, location_id, sku) DO NOTHING
                """;

        return jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement statement, int index) throws SQLException {
                Inventory inventory = inventories.get(index);
                statement.setString(1, inventory.getId());
                statement.setString(2, inventory.getEnterpriseId());
                statement.setString(3, inventory.getWarehouseId());
                statement.setString(4, inventory.getLocationId());
                statement.setString(5, inventory.getSku());
            }

            @Override
            public int getBatchSize() {
                return inventories.size();
            }
        });
    }

    /** Lấy lại các inventory vừa ghi bằng một SELECT để trả timestamp do database gán. */
    @Override
    public List<Inventory> findByIds(List<String> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        String placeholders = String.join(",", Collections.nCopies(ids.size(), "?"));
        String sql = """
                SELECT id, enterprise_id, warehouse_id, location_id, sku,
                       physical_quantity, reserved_quantity, defective_quantity,
                       pending_quantity, in_transit_quantity, version, created_at, updated_at
                FROM inventory WHERE id IN (
                """ + placeholders + ")";
        return jdbcTemplate.query(sql, inventoryMapper, ids.toArray());
    }

    /** Tạo dòng nếu chưa có; nếu đã có thì đọc và khóa dòng hiện tại trước khi cộng tồn. */
    public Inventory createOrFind(
            String enterpriseId,
            String warehouseId,
            String locationId,
            String sku
    ) {
        Optional<Inventory> created = insertInventory(enterpriseId, warehouseId, locationId, sku,
                " ON CONFLICT (enterprise_id, warehouse_id, location_id, sku) DO NOTHING ");
        if (created.isPresent()) {
            return created.get();
        }
        // Chỉ đọc khóa dòng khi vị trí đã tồn tại; tránh UPDATE giả và vẫn tuần tự hóa lần nhập.
        return findByPositionForUpdate(enterpriseId, warehouseId, locationId, sku)
                .orElseThrow(() -> new IllegalStateException("Inventory conflict row was not found"));
    }

    /** Đọc và khóa dòng theo khóa nghiệp vụ sau khi INSERT gặp xung đột duy nhất. */
    private Optional<Inventory> findByPositionForUpdate(String enterpriseId, String warehouseId,
                                                        String locationId, String sku) {
        String sql = """
                SELECT id, enterprise_id, warehouse_id, location_id, sku,
                       physical_quantity, reserved_quantity, defective_quantity,
                       pending_quantity, in_transit_quantity, version, created_at, updated_at
                FROM inventory
                WHERE enterprise_id = ? AND warehouse_id = ? AND location_id = ? AND sku = ?
                FOR UPDATE
                """;
        return queryOne(sql, enterpriseId, warehouseId, locationId, sku);
    }

    /** Phần INSERT dùng chung để tránh lệch cấu trúc giữa tạo mới và tạo-hoặc-lấy. */
    private Optional<Inventory> insertInventory(String enterpriseId, String warehouseId,
                                                String locationId, String sku, String onConflict) {
        String sql = """
            INSERT INTO inventory (
                id,
                enterprise_id,
                warehouse_id,
                location_id,
                sku,
                physical_quantity,
                reserved_quantity,
                defective_quantity,
                pending_quantity,
                in_transit_quantity,
                version,
                created_at,
                updated_at
            )
            VALUES (
                ?, ?, ?, ?, ?,
                0, 0, 0, 0, 0,
                0,
                CURRENT_TIMESTAMP,
                CURRENT_TIMESTAMP
            )
            """ + onConflict + """
            RETURNING
                id,
                enterprise_id,
                warehouse_id,
                location_id,
                sku,
                physical_quantity,
                reserved_quantity,
                defective_quantity,
                pending_quantity,
                in_transit_quantity,
                version,
                created_at,
                updated_at
            """;

        // Tạo UUID ở ứng dụng để mọi khóa chính dùng cùng kiểu chuỗi.
        return queryOne(
                sql,
                UUID.randomUUID().toString(),
                enterpriseId,
                warehouseId,
                locationId,
                sku
        );
    }

    /** Đọc tối đa một dòng mà không tạo danh sách trung gian cho các truy vấn theo khóa duy nhất. */
    private Optional<Inventory> queryOne(String sql, Object... args) {
        Inventory inventory = jdbcTemplate.query(sql, resultSet -> {
            if (!resultSet.next()) {
                return null;
            }
            return inventoryMapper.mapRow(resultSet, 0);
        }, args);
        return Optional.ofNullable(inventory);
    }
    /** Cộng hàng nhập vào tồn vật lý và tăng version trong cùng một câu UPDATE nguyên tử. */
    public Inventory receive(
            String inventoryId,
            BigDecimal quantity
    ) {

        String sql = """
            UPDATE inventory
            SET
                physical_quantity = physical_quantity + ?,
                updated_at = CURRENT_TIMESTAMP,
                version = version + 1
            WHERE id = ?
            RETURNING
                id,
                enterprise_id,
                warehouse_id,
                location_id,
                sku,
                physical_quantity,
                reserved_quantity,
                defective_quantity,
                pending_quantity,
                in_transit_quantity,
                version,
                created_at,
                updated_at
            """;

        return jdbcTemplate.queryForObject(
                sql,
                inventoryMapper,
                quantity,
                inventoryId
        );
    }
    /** Chỉ giữ hàng nếu lượng khả dụng ngay tại thời điểm cập nhật đủ đáp ứng yêu cầu. */
    public Optional<Inventory> reserve(
            String inventoryId,
            BigDecimal quantity
    ) {

        String sql = """
            UPDATE inventory
            SET
                reserved_quantity = reserved_quantity + ?,
                updated_at = CURRENT_TIMESTAMP,
                version = version + 1
            WHERE id = ?
              AND (
                    physical_quantity
                    - reserved_quantity
                    - defective_quantity
                    - pending_quantity
                  ) >= ?
            RETURNING
                id,
                enterprise_id,
                warehouse_id,
                location_id,
                sku,
                physical_quantity,
                reserved_quantity,
                defective_quantity,
                pending_quantity,
                in_transit_quantity,
                version,
                created_at,
                updated_at
            """;

        return jdbcTemplate
                .query(
                        sql,
                        inventoryMapper,
                        quantity,
                        inventoryId,
                        quantity
                )
                .stream()
                .findFirst();
    }

    /** Chỉ bỏ giữ nếu lượng đã giữ không nhỏ hơn lượng cần nhả. */
    public Optional<Inventory> release(
            String inventoryId,
            BigDecimal quantity
    ) {

        String sql = """
            UPDATE inventory
            SET
                reserved_quantity = reserved_quantity - ?,
                updated_at = CURRENT_TIMESTAMP,
                version = version + 1
            WHERE id = ?
              AND reserved_quantity >= ?
            RETURNING
                id,
                enterprise_id,
                warehouse_id,
                location_id,
                sku,
                physical_quantity,
                reserved_quantity,
                defective_quantity,
                pending_quantity,
                in_transit_quantity,
                version,
                created_at,
                updated_at
            """;

        return jdbcTemplate
                .query(
                        sql,
                        inventoryMapper,
                        quantity,
                        inventoryId,
                        quantity
                )
                .stream()
                .findFirst();
    }
    /** Xuất hàng bằng một UPDATE có điều kiện để tránh làm tồn khả dụng âm khi có tranh chấp. */
    public Optional<Inventory> issue(
            String inventoryId,
            BigDecimal quantity
    ) {

        String sql = """
            UPDATE inventory
            SET
                physical_quantity = physical_quantity - ?,
                updated_at = CURRENT_TIMESTAMP,
                version = version + 1
            WHERE id = ?
              AND (
                    physical_quantity
                    - reserved_quantity
                    - defective_quantity
                    - pending_quantity
                  ) >= ?
            RETURNING
                id,
                enterprise_id,
                warehouse_id,
                location_id,
                sku,
                physical_quantity,
                reserved_quantity,
                defective_quantity,
                pending_quantity,
                in_transit_quantity,
                version,
                created_at,
                updated_at
            """;

        return jdbcTemplate
                .query(
                        sql,
                        inventoryMapper,
                        quantity,
                        inventoryId,
                        quantity
                )
                .stream()
                .findFirst();
    }

    /** Chỉ chấp nhận số kiểm kê mới nếu vẫn đủ chứa phần giữ, lỗi và đang chờ. */
    public Optional<Inventory> adjust(
            String inventoryId,
            BigDecimal actualQuantity
    ) {

        String sql = """
            UPDATE inventory
            SET
                physical_quantity = ?,
                updated_at = CURRENT_TIMESTAMP,
                version = version + 1
            WHERE id = ?
              AND ? >= (
                    reserved_quantity
                    + defective_quantity
                    + pending_quantity
                  )
            RETURNING
                id,
                enterprise_id,
                warehouse_id,
                location_id,
                sku,
                physical_quantity,
                reserved_quantity,
                defective_quantity,
                pending_quantity,
                in_transit_quantity,
                version,
                created_at,
                updated_at
            """;

        return jdbcTemplate
                .query(
                        sql,
                        inventoryMapper,

                        // Giá trị tồn vật lý mới sau kiểm kê.
                        actualQuantity,

                        // ID của vị trí tồn cần điều chỉnh.
                        inventoryId,

                        // Kiểm tra tồn mới vẫn đủ chứa hàng đã giữ, hàng lỗi và hàng đang chờ.
                        actualQuantity
                )
                .stream()
                .findFirst();
    }
}

