package com.dtthuan3.ecommerce.inventoryservice.repository.customrepo;

import com.dtthuan3.ecommerce.inventoryservice.entity.Inventory;
import com.dtthuan3.ecommerce.inventoryservice.mapper.InventoryRowMapper;
import com.dtthuan3.ecommerce.inventoryservice.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
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
public class JdbcInventoryRepository implements InventoryRepository {

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

    /** Tìm các vị trí có SKU này ở mọi kho theo trang để không tải hết kết quả vào bộ nhớ. */
    @Override
    public Page<Inventory> findBySku(String sku, Pageable pageable) {
        return queryInventoryPage("WHERE sku = ?", "warehouse_id, location_id, id", pageable, sku);
    }

    /** Tìm các vị trí hàng thuộc một kho theo trang. */
    @Override
    public Page<Inventory> findByWarehouse(String warehouseId, Pageable pageable) {
        return queryInventoryPage("WHERE warehouse_id = ?", "location_id, sku, id", pageable, warehouseId);
    }

    /** Tìm các vị trí tồn trong một khu vực lưu trữ theo trang. */
    @Override
    public Page<Inventory> findByLocation(String locationId, Pageable pageable) {
        return queryInventoryPage("WHERE location_id = ?", "warehouse_id, sku, id", pageable, locationId);
    }

    /** Chỉ trả hàng còn bán được: tồn vật lý trừ hàng giữ, hàng lỗi và hàng đang chờ. */
    @Override
    public Page<Inventory> findAvailable(String sku, String warehouseId, String locationId, Pageable pageable) {
        // Tham số tùy chọn được thêm vào SQL bằng placeholder, không ghép giá trị trực tiếp vào câu truy vấn.
        StringBuilder where = new StringBuilder("WHERE physical_quantity - reserved_quantity - defective_quantity - pending_quantity > 0");
        List<Object> args = new ArrayList<>();
        addOptionalFilter(where, args, "sku", sku);
        addOptionalFilter(where, args, "warehouse_id", warehouseId);
        addOptionalFilter(where, args, "location_id", locationId);
        return queryInventoryPage(where.toString(), "warehouse_id, location_id, sku, id", pageable, args.toArray());
    }

    /** Dùng chung điều kiện/truy vấn; orderBy chỉ nhận hằng số nội bộ, không nhận từ request. */
    private Page<Inventory> queryInventoryPage(String where, String orderBy, Pageable pageable, Object... args) {
        String sql = """
                SELECT id, enterprise_id, warehouse_id, location_id, sku,
                       physical_quantity, reserved_quantity, defective_quantity,
                       pending_quantity, in_transit_quantity, version, created_at, updated_at
                FROM inventory
                """ + where + " ORDER BY " + orderBy + " LIMIT ? OFFSET ?";

        List<Object> pageArgs = new ArrayList<>(Arrays.asList(args));
        pageArgs.add(pageable.getPageSize());
        pageArgs.add(pageable.getOffset());
        List<Inventory> content = jdbcTemplate.query(sql, inventoryMapper, pageArgs.toArray());

        return PageableExecutionUtils.getPage(content, pageable, () -> {
            Long total = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM inventory " + where,
                    Long.class,
                    args);
            return total == null ? 0 : total;
        });
    }

    private void addOptionalFilter(StringBuilder where, List<Object> args, String column, String value) {
        if (value != null && !value.isBlank()) {
            where.append(" AND ").append(column).append(" = ?");
            args.add(value.trim());
        }
    }

    /** Tìm duy nhất vị trí tồn theo doanh nghiệp, kho, khu vực lưu trữ và SKU. */
    @Override
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

        return queryOne(sql, enterpriseId, warehouseId, locationId, sku);
    }

    /**
     * Tải và khóa các SKU nguồn bằng một SELECT để dispatch nhiều dòng không phát sinh N+1 query.
     * ORDER BY id khiến các transaction cùng tập tồn lấy khóa theo thứ tự thống nhất.
     */
    @Override
    public List<Inventory> findByPositionForUpdate(String enterpriseId, String warehouseId,
                                                   String locationId, List<String> skus) {
        if (skus == null || skus.isEmpty()) {
            return List.of();
        }
        String skuPlaceholders = String.join(",", Collections.nCopies(skus.size(), "?"));
        String sql = """
                SELECT id, enterprise_id, warehouse_id, location_id, sku,
                       physical_quantity, reserved_quantity, defective_quantity,
                       pending_quantity, in_transit_quantity, version, created_at, updated_at
                FROM inventory
                WHERE enterprise_id = ? AND warehouse_id = ? AND location_id = ?
                  AND sku IN (%s)
                ORDER BY id
                FOR UPDATE
                """.formatted(skuPlaceholders);
        List<Object> parameters = new ArrayList<>(3 + skus.size());
        parameters.add(enterpriseId);
        parameters.add(warehouseId);
        parameters.add(locationId);
        parameters.addAll(skus);
        return jdbcTemplate.query(sql, inventoryMapper, parameters.toArray());
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

    /** Khóa toàn bộ dòng theo thứ tự ID cố định để nhận điều chuyển hai chiều tránh deadlock. */
    @Override
    public List<Inventory> findByIdsForUpdate(List<String> ids) {
        List<String> orderedIds = ids == null ? List.of() : ids.stream().distinct().sorted().toList();
        if (orderedIds.isEmpty()) return List.of();
        String placeholders = String.join(",", Collections.nCopies(orderedIds.size(), "?"));
        String sql = """
                SELECT id, enterprise_id, warehouse_id, location_id, sku,
                       physical_quantity, reserved_quantity, defective_quantity,
                       pending_quantity, in_transit_quantity, version, created_at, updated_at
                FROM inventory WHERE id IN (
                """ + placeholders + ") ORDER BY id FOR UPDATE";
        return jdbcTemplate.query(sql, inventoryMapper, orderedIds.toArray());
    }

    /** Tạo balance đích nếu thiếu mà chưa khóa balance đã tồn tại. */
    @Override
    public void ensureExists(String enterpriseId, String warehouseId, String locationId, String sku) {
        insertInventory(enterpriseId, warehouseId, locationId, sku,
                " ON CONFLICT (enterprise_id, warehouse_id, location_id, sku) DO NOTHING ");
    }

    /** Tạo dòng nếu chưa có; nếu đã có thì đọc và khóa dòng hiện tại trước khi cộng tồn. */
    @Override
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
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy dòng Inventory đang bị xung đột"));
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
    @Override
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
              AND ? > 0
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
                inventoryId,
                quantity
        );
    }

    /** Trừ lượng đã đối soát khỏi hàng đang trên đường, không cho bucket in-transit xuống âm. */
    @Override
    public Optional<Inventory> decreaseInTransit(String inventoryId, BigDecimal quantity) {
        return jdbcTemplate.query("""
                        UPDATE inventory
                        SET in_transit_quantity = in_transit_quantity - ?,
                            updated_at = CURRENT_TIMESTAMP,
                            version = version + 1
                        WHERE id = ? AND in_transit_quantity >= ? AND ? > 0
                        RETURNING id, enterprise_id, warehouse_id, location_id, sku,
                                  physical_quantity, reserved_quantity, defective_quantity,
                                  pending_quantity, in_transit_quantity, version, created_at, updated_at
                        """, inventoryMapper, quantity, inventoryId, quantity, quantity)
                .stream().findFirst();
    }

    /**
     * Ghi tổng hàng đã nhận vào tồn vật lý, đồng thời đưa hàng hỏng và hàng chờ vào bucket riêng.
     * Một câu UPDATE duy nhất bảo đảm không có trạng thái chỉ tăng một phần số lượng.
     */
    @Override
    public Inventory receiveClassified(String inventoryId, BigDecimal goodQuantity,
                                       BigDecimal damagedQuantity, BigDecimal inspectionQuantity) {
        BigDecimal physicalIncrease = goodQuantity.add(damagedQuantity).add(inspectionQuantity);
        return jdbcTemplate.queryForObject("""
                        UPDATE inventory
                        SET physical_quantity = physical_quantity + ?,
                            defective_quantity = defective_quantity + ?,
                            pending_quantity = pending_quantity + ?,
                            updated_at = CURRENT_TIMESTAMP,
                            version = version + 1
                        WHERE id = ?
                          AND ? >= 0
                          AND ? >= 0
                          AND ? >= 0
                          AND (? + ? + ?) > 0
                        RETURNING id, enterprise_id, warehouse_id, location_id, sku,
                                  physical_quantity, reserved_quantity, defective_quantity,
                                  pending_quantity, in_transit_quantity, version, created_at, updated_at
                        """,
                inventoryMapper, physicalIncrease, damagedQuantity, inspectionQuantity, inventoryId,
                goodQuantity, damagedQuantity, inspectionQuantity,
                goodQuantity, damagedQuantity, inspectionQuantity);
    }

    /**
     * Chốt phân loại hàng trả bằng một UPDATE nguyên tử: tổng hàng đã kiểm tra giảm pending,
     * phần hỏng tăng defective; physical_quantity không đổi vì hàng đã được nhận vào kho trước đó.
     */
    @Override
    public Optional<Inventory> inspectPendingReturn(String inventoryId, BigDecimal inspectedQuantity,
                                                     BigDecimal damagedQuantity) {
        return jdbcTemplate.query("""
                        UPDATE inventory
                        SET pending_quantity = pending_quantity - ?,
                            defective_quantity = defective_quantity + ?,
                            updated_at = CURRENT_TIMESTAMP,
                            version = version + 1
                        WHERE id = ?
                          AND pending_quantity >= ?
                          AND ? > 0
                          AND ? >= 0
                          AND ? <= ?
                        RETURNING id, enterprise_id, warehouse_id, location_id, sku,
                                  physical_quantity, reserved_quantity, defective_quantity,
                                  pending_quantity, in_transit_quantity, version, created_at, updated_at
                        """,
                inventoryMapper, inspectedQuantity, damagedQuantity, inventoryId, inspectedQuantity,
                inspectedQuantity, damagedQuantity, damagedQuantity, inspectedQuantity)
                .stream().findFirst();
    }

    /** Chỉ giữ hàng nếu lượng khả dụng ngay tại thời điểm cập nhật đủ đáp ứng yêu cầu. */
    @Override
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
              AND ? > 0
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
                        quantity,
                        quantity
                )
                .stream()
                .findFirst();
    }

    /** Chỉ bỏ giữ nếu lượng đã giữ không nhỏ hơn lượng cần nhả. */
    @Override
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
              AND ? > 0
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
                        quantity,
                        quantity
                )
                .stream()
                .findFirst();
    }
    /** Xuất hàng bằng một UPDATE có điều kiện để tránh làm tồn khả dụng âm khi có tranh chấp. */
    @Override
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
              AND ? > 0
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
                        quantity,
                        quantity
                )
                .stream()
                .findFirst();
    }

    /** Xuất đúng lượng đã giữ; điều kiện SQL bảo vệ cả tồn vật lý lẫn phần reservation còn lại. */
    @Override
    public Optional<Inventory> dispatchReserved(String inventoryId, BigDecimal quantity) {
        String sql = """
            UPDATE inventory
            SET physical_quantity = physical_quantity - ?,
                reserved_quantity = reserved_quantity - ?,
                updated_at = CURRENT_TIMESTAMP,
                version = version + 1
            WHERE id = ? AND physical_quantity >= ? AND reserved_quantity >= ? AND ? > 0
            RETURNING id, enterprise_id, warehouse_id, location_id, sku, physical_quantity,
                      reserved_quantity, defective_quantity, pending_quantity, in_transit_quantity,
                      version, created_at, updated_at
            """;
        return jdbcTemplate.query(sql, inventoryMapper, quantity, quantity, inventoryId,
                quantity, quantity, quantity).stream().findFirst();
    }

    /**
     * Giảm tồn vật lý và tăng in-transit cho tối đa 200 dòng trong một câu UPDATE.
     * Mỗi dòng chỉ được cập nhật nếu tồn khả dụng tại database vẫn đủ; caller rollback toàn bộ
     * transaction nếu RETURNING không trả đủ số dòng.
     */
    @Override
    public List<Inventory> dispatchTransferBatch(List<TransferDispatchUpdate> updates) {
        if (updates == null || updates.isEmpty()) {
            return List.of();
        }
        String valueRows = String.join(",", Collections.nCopies(updates.size(), "(?, ?)"));
        String sql = """
                WITH requested(inventory_id, quantity) AS (VALUES %s)
                UPDATE inventory AS i
                SET physical_quantity = i.physical_quantity - requested.quantity,
                    in_transit_quantity = i.in_transit_quantity + requested.quantity,
                    updated_at = CURRENT_TIMESTAMP,
                    version = i.version + 1
                FROM requested
                WHERE i.id = requested.inventory_id
                  AND requested.quantity > 0
                  AND i.physical_quantity - i.reserved_quantity
                      - i.defective_quantity - i.pending_quantity >= requested.quantity
                RETURNING i.id, i.enterprise_id, i.warehouse_id, i.location_id, i.sku,
                          i.physical_quantity, i.reserved_quantity, i.defective_quantity,
                          i.pending_quantity, i.in_transit_quantity, i.version, i.created_at, i.updated_at
                """.formatted(valueRows);
        List<Object> parameters = new ArrayList<>(updates.size() * 2);
        for (TransferDispatchUpdate update : updates) {
            parameters.add(update.inventoryId());
            parameters.add(update.quantity());
        }
        return jdbcTemplate.query(sql, inventoryMapper, parameters.toArray());
    }

    /** Chỉ chấp nhận số kiểm kê mới nếu vẫn đủ chứa phần giữ, lỗi và đang chờ. */
    @Override
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
              AND ? >= 0
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
                        actualQuantity,

                        // Không cho phép lượng kiểm kê âm dù repository được gọi nội bộ.
                        actualQuantity
                )
                .stream()
                .findFirst();
    }

    /** Áp dụng kết quả kiểm kê theo từng bucket với optimistic guard để không ghi đè movement mới. */
    @Override
    public Optional<Inventory> adjustCountedBuckets(String inventoryId, BigDecimal goodQuantity,
                                                     BigDecimal defectiveQuantity, BigDecimal pendingQuantity,
                                                     long expectedVersion) {
        BigDecimal physicalQuantity = goodQuantity.add(defectiveQuantity).add(pendingQuantity);
        String sql = """
                UPDATE inventory
                SET physical_quantity = ?, defective_quantity = ?, pending_quantity = ?,
                    updated_at = CURRENT_TIMESTAMP, version = version + 1
                WHERE id = ? AND version = ? AND reserved_quantity <= ?
                  AND ? >= 0 AND ? >= 0 AND ? >= 0
                RETURNING id, enterprise_id, warehouse_id, location_id, sku,
                          physical_quantity, reserved_quantity, defective_quantity,
                          pending_quantity, in_transit_quantity, version, created_at, updated_at
                """;
        return jdbcTemplate.query(sql, inventoryMapper, physicalQuantity, defectiveQuantity, pendingQuantity,
                        inventoryId, expectedVersion, goodQuantity, goodQuantity, defectiveQuantity, pendingQuantity)
                .stream().findFirst();
    }
}

