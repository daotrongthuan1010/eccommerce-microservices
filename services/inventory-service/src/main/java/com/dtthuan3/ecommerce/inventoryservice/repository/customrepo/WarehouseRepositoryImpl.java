package com.dtthuan3.ecommerce.inventoryservice.repository.customrepo;

import com.dtthuan3.ecommerce.inventoryservice.repository.WarehouseRepository;
import com.dtthuan3.ecommerce.inventoryservice.repository.model.WarehouseRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * SQL adapter cho Warehouse.
 * Truy vấn chọn cột tường minh để tránh hydrate entity không cần thiết; Hibernate vẫn quản lý DDL.
 */
@Repository
@RequiredArgsConstructor
public class WarehouseRepositoryImpl implements WarehouseRepository {
    private static final RowMapper<WarehouseRecord> ROW_MAPPER = (rs, row) -> new WarehouseRecord(
            rs.getString("id"), rs.getString("enterprise_id"), rs.getString("code"),
            rs.getString("name"), rs.getString("address"), rs.getString("status"),
            rs.getTimestamp("created_at").toInstant(), rs.getTimestamp("updated_at").toInstant());

    private final JdbcTemplate jdbcTemplate;

    /** Đọc dữ liệu kho cần cho response hoặc cập nhật thông tin. */
    @Override
    public Optional<WarehouseRecord> findById(String id) {
        return jdbcTemplate.query("""
                SELECT id, enterprise_id, code, name, address, status, created_at, updated_at
                FROM inventory_warehouse WHERE id = ?
                """, ROW_MAPPER, id).stream().findFirst();
    }

    /** Khóa dòng kho trong lúc caller kiểm tra trạng thái rồi ghi nhận nghiệp vụ liên quan. */
    @Override
    public Optional<WarehouseRecord> findByIdForShare(String id) {
        return jdbcTemplate.query("""
                SELECT id, enterprise_id, code, name, address, status, created_at, updated_at
                FROM inventory_warehouse WHERE id = ? FOR SHARE
                """, ROW_MAPPER, id).stream().findFirst();
    }

    /** Trả các kho thuộc đúng enterprise theo thứ tự mã kho. */
    @Override
    public List<WarehouseRecord> findByEnterpriseId(String enterpriseId) {
        return jdbcTemplate.query("""
                SELECT id, enterprise_id, code, name, address, status, created_at, updated_at
                FROM inventory_warehouse WHERE enterprise_id = ? ORDER BY code, id
                """, ROW_MAPPER, enterpriseId);
    }

    /** Tạo kho ở trạng thái ban đầu ACTIVE; ID/code trùng được database báo về service. */
    @Override
    public void insert(WarehouseRecord warehouse) {
        jdbcTemplate.update("""
                INSERT INTO inventory_warehouse
                    (id, enterprise_id, code, name, address, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, warehouse.id(), warehouse.enterpriseId(), warehouse.code(), warehouse.name(),
                warehouse.address(), warehouse.status(), Timestamp.from(warehouse.createdAt()),
                Timestamp.from(warehouse.updatedAt()));
    }

    /** Cập nhật tên/mã/địa chỉ và thời điểm sửa, không thay đổi enterprise hay trạng thái. */
    @Override
    public void updateDetails(String id, String code, String name, String address, Instant updatedAt) {
        jdbcTemplate.update("""
                UPDATE inventory_warehouse SET code = ?, name = ?, address = ?, updated_at = ? WHERE id = ?
                """, code, name, address, Timestamp.from(updatedAt), id);
    }

    /** Bật/tắt kho mà không xóa dòng dữ liệu được các chứng từ cũ tham chiếu. */
    @Override
    public void updateStatus(String id, String status, Instant updatedAt) {
        jdbcTemplate.update("""
                UPDATE inventory_warehouse SET status = ?, updated_at = ? WHERE id = ?
                """, status, Timestamp.from(updatedAt), id);
    }
}
