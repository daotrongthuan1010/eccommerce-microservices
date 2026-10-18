package com.dtthuan3.ecommerce.inventoryservice.repository.customrepo;

import com.dtthuan3.ecommerce.inventoryservice.repository.SupplierReceiptRepository;
import com.dtthuan3.ecommerce.inventoryservice.repository.model.SupplierReceiptHeader;
import com.dtthuan3.ecommerce.inventoryservice.repository.model.SupplierReceiptLineRecord;
import com.dtthuan3.ecommerce.inventoryservice.dto.response.SupplierReceiptSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Hiện thực repository bằng SQL/JdbcTemplate cho các thao tác phiếu nhập.
 * Cách này chỉ lấy các cột cần thiết, dùng câu lệnh upsert rõ ràng và batch insert
 * thay vì khởi tạo persistence context, entity proxy hoặc phát sinh nhiều lệnh ORM.
 */
@Repository
@RequiredArgsConstructor
public class SupplierReceiptRepositoryImpl implements SupplierReceiptRepository {
    private static final String FIND_HEADER_SQL = """
            SELECT id, warehouse_id, supplier_ref, operation_id, payload_hash, status, version, created_at
            FROM inventory_supplier_receipt
            WHERE operation_id = ?
            """;

    private static final RowMapper<SupplierReceiptHeader> HEADER_ROW_MAPPER = (rs, rowNumber) ->
            new SupplierReceiptHeader(
                    rs.getString("id"),
                    rs.getString("warehouse_id"),
                    rs.getString("supplier_ref"),
                    rs.getString("operation_id"),
                    rs.getString("payload_hash"),
                    rs.getString("status"),
                    rs.getLong("version"),
                    rs.getTimestamp("created_at").toInstant());

    private static final RowMapper<SupplierReceiptLineRecord> LINE_ROW_MAPPER = (rs, rowNumber) ->
            new SupplierReceiptLineRecord(
                    rs.getString("id"),
                    rs.getInt("line_number"),
                    rs.getString("sku_id"),
                    rs.getString("uom"),
                    rs.getBigDecimal("expected_qty"),
                    rs.getString("lot_code"),
                    rs.getBigDecimal("actual_qty"),
                    rs.getBigDecimal("good_qty"),
                    rs.getBigDecimal("damaged_qty"),
                    rs.getBigDecimal("inspection_qty"));

    private final JdbcTemplate jdbcTemplate;

    /** Dùng truy vấn theo unique key để tra cứu kết quả idempotency với đúng một dòng header. */
    @Override
    public Optional<SupplierReceiptHeader> findByOperationId(String operationId) {
        List<SupplierReceiptHeader> matches = jdbcTemplate.query(FIND_HEADER_SQL, HEADER_ROW_MAPPER, operationId);
        return matches.stream().findFirst();
    }

    /**
     * Đọc đúng một phiếu bằng primary key; chỉ lấy các cột của header cần cho response.
     * Không dùng SELECT FOR UPDATE vì đây là API đọc và không được giữ khóa ghi.
     */
    @Override
    public Optional<SupplierReceiptHeader> findById(String receiptId) {
        List<SupplierReceiptHeader> matches = jdbcTemplate.query("""
                        SELECT id, warehouse_id, supplier_ref, operation_id, payload_hash, status, version, created_at
                        FROM inventory_supplier_receipt
                        WHERE id = ?
                        """,
                HEADER_ROW_MAPPER, receiptId);
        return matches.stream().findFirst();
    }

    /**
     * Trả danh sách tóm tắt có phân trang. LEFT JOIN và COUNT cho biết số dòng mỗi phiếu,
     * nhưng không kéo toàn bộ dữ liệu SKU/chứng từ của mọi phiếu vào bộ nhớ.
     */
    @Override
    public Page<SupplierReceiptSummaryResponse> findByWarehouseId(String warehouseId, Pageable pageable) {
        List<SupplierReceiptSummaryResponse> content = jdbcTemplate.query("""
                        SELECT receipt.id, receipt.warehouse_id, receipt.supplier_ref, receipt.status,
                               receipt.version, receipt.operation_id, receipt.created_at,
                               COUNT(line.id) AS line_count
                        FROM inventory_supplier_receipt receipt
                        LEFT JOIN inventory_supplier_receipt_line line ON line.receipt_id = receipt.id
                        WHERE receipt.warehouse_id = ?
                        GROUP BY receipt.id, receipt.warehouse_id, receipt.supplier_ref, receipt.status,
                                 receipt.version, receipt.operation_id, receipt.created_at
                        ORDER BY receipt.created_at DESC, receipt.id DESC
                        LIMIT ? OFFSET ?
                        """,
                (rs, rowNumber) -> new SupplierReceiptSummaryResponse(
                        rs.getString("id"),
                        rs.getString("warehouse_id"),
                        rs.getString("supplier_ref"),
                        rs.getString("status"),
                        rs.getLong("version"),
                        rs.getString("operation_id"),
                        rs.getLong("line_count"),
                        rs.getTimestamp("created_at").toInstant()),
                warehouseId, pageable.getPageSize(), pageable.getOffset());

        return PageableExecutionUtils.getPage(content, pageable, () -> {
            Long total = jdbcTemplate.queryForObject("""
                    SELECT COUNT(*)
                    FROM inventory_supplier_receipt
                    WHERE warehouse_id = ?
                    """, Long.class, warehouseId);
            return total == null ? 0 : total;
        });
    }

    /**
     * Khóa header tới khi transaction kết thúc để hai lần cập nhật không cùng ghi đè một version.
     * Phương thức này chỉ được gọi bên trong transaction do application service mở.
     */
    @Override
    public Optional<SupplierReceiptHeader> findByIdForUpdate(String receiptId) {
        List<SupplierReceiptHeader> matches = jdbcTemplate.query("""
                        SELECT id, warehouse_id, supplier_ref, operation_id, payload_hash, status, version, created_at
                        FROM inventory_supplier_receipt
                        WHERE id = ?
                        FOR UPDATE
                        """,
                HEADER_ROW_MAPPER, receiptId);
        return matches.stream().findFirst();
    }

    /**
     * PostgreSQL xử lý tranh chấp operationId tại unique index: request thắng chèn header,
     * request còn lại đợi transaction thắng kết thúc rồi nhận row count bằng 0.
     */
    @Override
    public boolean insertHeaderIfAbsent(SupplierReceiptHeader header) {
        int affected = jdbcTemplate.update("""
                        INSERT INTO inventory_supplier_receipt
                            (id, warehouse_id, supplier_ref, operation_id, payload_hash, status, version, created_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                        ON CONFLICT (operation_id) DO NOTHING
                        """,
                header.id(), header.warehouseId(), header.supplierRef(), header.operationId(),
                header.payloadHash(), header.status(), header.version(), Timestamp.from(header.createdAt()));
        return affected == 1;
    }

    /** Ghi tối đa 500 dòng trong batch; lỗi một dòng làm transaction của caller rollback toàn phiếu. */
    @Override
    public void insertLines(String receiptId, List<SupplierReceiptLineRecord> lines) {
        if (lines.isEmpty()) return;
        jdbcTemplate.batchUpdate("""
                        INSERT INTO inventory_supplier_receipt_line
                            (id, receipt_id, line_number, sku_id, uom, expected_qty, lot_code)
                        VALUES (?, ?, ?, ?, ?, ?, ?)
                        """,
                new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement statement, int index) throws SQLException {
                        SupplierReceiptLineRecord line = lines.get(index);
                        statement.setString(1, line.lineId());
                        statement.setString(2, receiptId);
                        statement.setInt(3, line.lineNumber());
                        statement.setString(4, line.skuId());
                        statement.setString(5, line.uom());
                        statement.setBigDecimal(6, line.expectedQty());
                        statement.setString(7, line.lotCode());
                    }

                    @Override
                    public int getBatchSize() {
                        return lines.size();
                    }
                });
    }

    /** Tạo ID liên kết trong Inventory; assetId vẫn do File Service sở hữu và được giữ nguyên. */
    @Override
    public void insertDocuments(String receiptId, List<String> assetIds) {
        if (assetIds.isEmpty()) return;
        jdbcTemplate.batchUpdate("""
                        INSERT INTO inventory_supplier_receipt_document (id, receipt_id, asset_id)
                        VALUES (?, ?, ?)
                        """,
                new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement statement, int index) throws SQLException {
                        statement.setString(1, UUID.randomUUID().toString());
                        statement.setString(2, receiptId);
                        statement.setString(3, assetIds.get(index));
                    }

                    @Override
                    public int getBatchSize() {
                        return assetIds.size();
                    }
                });
    }

    /** Đọc các cột cần thiết để dựng response dòng phiếu, không hydrate toàn bộ entity/header. */
    @Override
    public List<SupplierReceiptLineRecord> findLines(String receiptId) {
        return jdbcTemplate.query("""
                        SELECT id, line_number, sku_id, uom, expected_qty, lot_code,
                               actual_qty, good_qty, damaged_qty, inspection_qty
                        FROM inventory_supplier_receipt_line
                        WHERE receipt_id = ?
                        ORDER BY line_number
                        """,
                LINE_ROW_MAPPER, receiptId);
    }

    /** Sắp xếp theo asset_id để response retry không phụ thuộc thứ tự vật lý của bảng. */
    @Override
    public List<String> findDocumentAssetIds(String receiptId) {
        return jdbcTemplate.query("""
                        SELECT asset_id
                        FROM inventory_supplier_receipt_document
                        WHERE receipt_id = ?
                        ORDER BY asset_id
                        """,
                (rs, rowNumber) -> rs.getString("asset_id"), receiptId);
    }

    /**
     * Compare-and-set version ở DB; status DRAFT là điều kiện để không sửa phiếu đã gửi/duyệt.
     * Khóa header trước đó giúp thông báo rõ version stale và tránh hai request cùng thành công.
     */
    @Override
    public boolean incrementVersion(String receiptId, long expectedVersion) {
        return jdbcTemplate.update("""
                        UPDATE inventory_supplier_receipt
                        SET version = version + 1
                        WHERE id = ? AND version = ? AND status = 'DRAFT'
                        """,
                receiptId, expectedVersion) == 1;
    }

    /** Batch update gọn từng dòng; mọi update nằm cùng transaction với version và evidence links. */
    @Override
    public void updateActuals(String receiptId, List<SupplierReceiptLineRecord> lines) {
        if (lines.isEmpty()) return;
        int[] counts = jdbcTemplate.batchUpdate("""
                        UPDATE inventory_supplier_receipt_line
                        SET actual_qty = ?, good_qty = ?, damaged_qty = ?, inspection_qty = ?
                        WHERE receipt_id = ? AND id = ?
                        """,
                new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement statement, int index) throws SQLException {
                        SupplierReceiptLineRecord line = lines.get(index);
                        statement.setBigDecimal(1, line.actualQty());
                        statement.setBigDecimal(2, line.goodQty());
                        statement.setBigDecimal(3, line.damagedQty());
                        statement.setBigDecimal(4, line.inspectionQty());
                        statement.setString(5, receiptId);
                        statement.setString(6, line.lineId());
                    }

                    @Override
                    public int getBatchSize() {
                        return lines.size();
                    }
                });
        for (int count : counts) {
            if (count == 0) {
                throw new IllegalStateException("Không cập nhật được một dòng của phiếu nhập " + receiptId);
            }
        }
    }

    /**
     * Chứng từ bằng chứng được nối thêm chứ không thay thế tệp đã gắn lúc tạo phiếu.
     * ON CONFLICT giúp retry không nhân đôi liên kết asset đã tồn tại.
     */
    @Override
    public void appendDocuments(String receiptId, List<String> assetIds) {
        if (assetIds.isEmpty()) return;
        jdbcTemplate.batchUpdate("""
                        INSERT INTO inventory_supplier_receipt_document (id, receipt_id, asset_id)
                        VALUES (?, ?, ?)
                        ON CONFLICT (receipt_id, asset_id) DO NOTHING
                        """,
                new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement statement, int index) throws SQLException {
                        statement.setString(1, UUID.randomUUID().toString());
                        statement.setString(2, receiptId);
                        statement.setString(3, assetIds.get(index));
                    }

                    @Override
                    public int getBatchSize() {
                        return assetIds.size();
                    }
                });
    }
}
