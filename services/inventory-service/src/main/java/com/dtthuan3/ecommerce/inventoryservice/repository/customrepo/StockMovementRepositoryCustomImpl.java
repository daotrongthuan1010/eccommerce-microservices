package com.dtthuan3.ecommerce.inventoryservice.repository.customrepo;

import com.dtthuan3.ecommerce.inventoryservice.dto.response.StockMovementResponse;
import com.dtthuan3.ecommerce.inventoryservice.entity.MovementType;
import com.dtthuan3.ecommerce.inventoryservice.entity.StockMovement;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Ghi/đọc lịch sử kho bằng SQL trực tiếp để chỉ lấy cột cần thiết và kiểm soát phân trang. */
@Repository
@RequiredArgsConstructor
public class StockMovementRepositoryCustomImpl implements StockMovementRepositoryCustom {

    private static final String INSERT_MOVEMENT_SQL = """
            INSERT INTO stock_movement (
                id, inventory_id, movement_type, quantity,
                physical_before, physical_after, reserved_before, reserved_after,
                reference_type, reference_id, reason, performed_by, performed_by_subject, created_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
            """;

    private final JdbcTemplate jdbcTemplate;

    /**
     * Ghi một biến động trong cùng giao dịch với cập nhật vị trí tồn.
     * CURRENT_TIMESTAMP lấy giờ từ cơ sở dữ liệu; nếu INSERT lỗi, service hoàn tác cả cập nhật tồn.
     */
    @Override
    public void insertMovement(StockMovement movement) {
        // Một movement dùng UPDATE đơn; chỉ giao dịch nhiều movement mới tạo batch JDBC.
        jdbcTemplate.update(INSERT_MOVEMENT_SQL,
                movement.getId(),
                movement.getInventory().getId(),
                movement.getMovementType().name(),
                movement.getQuantity(),
                movement.getPhysicalBefore(),
                movement.getPhysicalAfter(),
                movement.getReservedBefore(),
                movement.getReservedAfter(),
                movement.getReferenceType(),
                movement.getReferenceId(),
                movement.getReason(),
                movement.getPerformedBy(),
                movement.getPerformedBySubject());
    }

    /** Ghi một lô movement; lỗi ở bất kỳ phần tử nào làm transaction kho rollback toàn bộ. */
    @Override
    public void insertMovements(List<StockMovement> movements) {
        if (movements.isEmpty()) {
            return;
        }
        jdbcTemplate.batchUpdate(INSERT_MOVEMENT_SQL, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement statement, int index) throws SQLException {
                StockMovement movement = movements.get(index);
                statement.setString(1, movement.getId());
                statement.setString(2, movement.getInventory().getId());
                statement.setString(3, movement.getMovementType().name());
                statement.setBigDecimal(4, movement.getQuantity());
                statement.setBigDecimal(5, movement.getPhysicalBefore());
                statement.setBigDecimal(6, movement.getPhysicalAfter());
                statement.setBigDecimal(7, movement.getReservedBefore());
                statement.setBigDecimal(8, movement.getReservedAfter());
                statement.setString(9, movement.getReferenceType());
                statement.setString(10, movement.getReferenceId());
                statement.setString(11, movement.getReason());
                statement.setString(12, movement.getPerformedBy());
                statement.setString(13, movement.getPerformedBySubject());
            }

            @Override
            public int getBatchSize() {
                return movements.size();
            }
        });
    }

    /**
     * Tìm biến động theo các bộ lọc tùy chọn.
     * Giá trị lọc được truyền qua placeholder, không ghép dữ liệu người dùng trực tiếp vào SQL.
     * Chỉ chọn cột cần trả về; truy vấn COUNT riêng cung cấp tổng số bản ghi cho thông tin phân trang.
     */
    @Override
    public Page<StockMovementResponse> searchMovements(String inventoryId, MovementType type,
                                                       String referenceId, LocalDateTime from,
                                                       LocalDateTime to, Pageable pageable) {
        // Dùng một mệnh đề WHERE chung để thêm/bỏ từng bộ lọc mà không cần tạo nhiều câu SQL.
        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        List<Object> filterArgs = new ArrayList<>(5);
        if (inventoryId != null && !inventoryId.isBlank()) {
            where.append(" AND inventory_id = ?");
            filterArgs.add(inventoryId);
        }
        if (type != null) {
            where.append(" AND movement_type = ?");
            filterArgs.add(type.name());
        }
        if (referenceId != null && !referenceId.isBlank()) {
            where.append(" AND reference_id = ?");
            filterArgs.add(referenceId);
        }
        if (from != null) {
            where.append(" AND created_at >= ?");
            filterArgs.add(from);
        }
        if (to != null) {
            where.append(" AND created_at <= ?");
            filterArgs.add(to);
        }

        // Sắp xếp theo thời gian và ID giảm dần để thứ tự ổn định khi nhiều biến động cùng thời điểm.
        String selectSql = """
                SELECT id, inventory_id, movement_type, quantity,
                       physical_before, physical_after, reserved_before, reserved_after,
                       reference_type, reference_id, reason, performed_by,
                       performed_by_subject, created_at
                FROM stock_movement
                """ + where + " ORDER BY created_at DESC, id DESC LIMIT ? OFFSET ?";

        List<Object> pageArgs = new ArrayList<>(filterArgs);
        pageArgs.add(pageable.getPageSize());
        pageArgs.add(pageable.getOffset());
        List<StockMovementResponse> movements = jdbcTemplate.query(selectSql, (rs, rowNum) ->
                new StockMovementResponse(
                        rs.getString("id"),
                        rs.getString("inventory_id"),
                        MovementType.valueOf(rs.getString("movement_type")),
                        rs.getBigDecimal("quantity"),
                        rs.getBigDecimal("physical_before"),
                        rs.getBigDecimal("physical_after"),
                        rs.getBigDecimal("reserved_before"),
                        rs.getBigDecimal("reserved_after"),
                        rs.getString("reference_type"),
                        rs.getString("reference_id"),
                        rs.getString("reason"),
                        rs.getString("performed_by"),
                        rs.getString("performed_by_subject"),
                        rs.getTimestamp("created_at").toLocalDateTime()), pageArgs.toArray());

        // Chỉ chạy COUNT khi số dòng trả về chưa đủ để suy ra tổng số trang.
        return PageableExecutionUtils.getPage(movements, pageable, () -> {
            Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM stock_movement" + where,
                    Long.class, filterArgs.toArray());
            return total == null ? 0 : total;
        });
    }
}
