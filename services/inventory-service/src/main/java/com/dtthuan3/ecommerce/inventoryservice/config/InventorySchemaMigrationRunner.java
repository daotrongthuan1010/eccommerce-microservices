package com.dtthuan3.ecommerce.inventoryservice.config;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.DataAccessException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

import java.sql.SQLException;

/**
 * Chạy migration schema Inventory có version sau khi Hibernate tạo bảng/cột.
 * Hibernate vẫn sinh entity/table; runner này áp dụng FK/CHECK có truy vết version. Việc VALIDATE dữ liệu
 * lịch sử có thể quét bảng lớn nên chỉ chạy khi được bật rõ ràng trong một lần bảo trì có kiểm soát.
 */
@Component
@DependsOn("entityManagerFactory")
@Slf4j
public class InventorySchemaMigrationRunner implements InitializingBean {
    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    /** Tắt mặc định để startup của từng replica không tự quét lại các bảng nghiệp vụ lớn. */
    @Value("${inventory.schema.validate-constraints-on-startup}")
    private boolean validateConstraintsOnStartup;

    public InventorySchemaMigrationRunner(JdbcTemplate jdbcTemplate, PlatformTransactionManager transactionManager) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    /** Chạy migration version sau khi Hibernate tạo bảng; advisory lock chặn hai replica cùng migrate. */
    @Override
    public void afterPropertiesSet() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS inventory_schema_history (
                    version VARCHAR(32) PRIMARY KEY,
                    description VARCHAR(255) NOT NULL,
                    applied_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """);
        transactionTemplate.executeWithoutResult(status -> {
            jdbcTemplate.execute("SELECT pg_advisory_xact_lock(hashtext('inventory-schema-migrations'))");
            if (jdbcTemplate.queryForObject("SELECT COUNT(*) FROM inventory_schema_history WHERE version = 'V001'",
                    Integer.class) > 0) return;
            applyV001Constraints();
            jdbcTemplate.update("""
                    INSERT INTO inventory_schema_history (version, description)
                    VALUES ('V001', 'Inventory foreign keys and non-negative balance checks')
                    """);
        });
        // V002 bảo vệ ID Inventory của operation nhập/xuất mà không tạo quan hệ JPA trong entity.
        transactionTemplate.executeWithoutResult(status -> {
            jdbcTemplate.execute("SELECT pg_advisory_xact_lock(hashtext('inventory-schema-migrations'))");
            if (jdbcTemplate.queryForObject("SELECT COUNT(*) FROM inventory_schema_history WHERE version = 'V002'",
                    Integer.class) > 0) return;
            ensureForeignKeyNotValid("inventory_stock_operation", "fk_inventory_stock_operation_inventory",
                    "inventory_id", "inventory", "id");
            jdbcTemplate.update("""
                    INSERT INTO inventory_schema_history (version, description)
                    VALUES ('V002', 'Inventory stock operation idempotency reference')
                    """);
        });
        // V003 cấp thứ tự bền vững cho event mới; dữ liệu cũ giữ NULL và được phát trước theo timestamp cũ.
        transactionTemplate.executeWithoutResult(status -> {
            jdbcTemplate.execute("SELECT pg_advisory_xact_lock(hashtext('inventory-schema-migrations'))");
            if (jdbcTemplate.queryForObject("SELECT COUNT(*) FROM inventory_schema_history WHERE version = 'V003'",
                    Integer.class) > 0) return;
            jdbcTemplate.execute("CREATE SEQUENCE IF NOT EXISTS inventory_event_outbox_order_seq");
            jdbcTemplate.execute("ALTER TABLE inventory_event_outbox ADD COLUMN IF NOT EXISTS sequence_id BIGINT");
            jdbcTemplate.update("""
                    INSERT INTO inventory_schema_history (version, description)
                    VALUES ('V003', 'Deterministic ordering for new Inventory outbox events')
                    """);
        });
        if (validateConstraintsOnStartup) {
            log.warn("Đang bật validate constraint Inventory lúc startup; chỉ bật trên một replica trong cửa sổ bảo trì");
            validateForeignKeysWhenClean();
            validateBalanceChecksWhenClean();
        }
    }

    /** Migration V001: tạo FK và CHECK sau khi Hibernate đã dựng bảng/cột của Inventory. */
    private void applyV001Constraints() {
        ensureForeignKeyNotValid("inventory_location", "fk_inventory_location_warehouse",
                "warehouse_id", "inventory_warehouse", "id");
        ensureForeignKeyNotValid("inventory", "fk_inventory_warehouse",
                "warehouse_id", "inventory_warehouse", "id");
        ensureForeignKey("inventory_warehouse_access_grant", "fk_inventory_warehouse_access_warehouse",
                "warehouse_id", "inventory_warehouse", "id");
        ensureForeignKeyNotValid("inventory_supplier_receipt", "fk_supplier_receipt_warehouse",
                "warehouse_id", "inventory_warehouse", "id");
        ensureForeignKey("inventory_supplier_receipt_line", "fk_supplier_receipt_line_header",
                "receipt_id", "inventory_supplier_receipt", "id");
        ensureForeignKey("inventory_supplier_receipt_line", "fk_supplier_receipt_line_location",
                "location_id", "inventory_location", "id");
        ensureForeignKey("inventory_supplier_receipt_document", "fk_supplier_receipt_document_header",
                "receipt_id", "inventory_supplier_receipt", "id");
        ensureForeignKey("stock_movement", "fk_stock_movement_inventory",
                "inventory_id", "inventory", "id");
        ensureForeignKey("inventory_return_receipt", "fk_return_receipt_inventory",
                "inventory_id", "inventory", "id");
        ensureForeignKey("inventory_return_inspection", "fk_return_inspection_receipt",
                "receipt_id", "inventory_return_receipt", "id");
        ensureForeignKey("inventory_return_inspection", "fk_return_inspection_inventory",
                "inventory_id", "inventory", "id");
        ensureForeignKey("inventory_transfer", "fk_inventory_transfer_source_warehouse",
                "source_warehouse_id", "inventory_warehouse", "id");
        ensureForeignKey("inventory_transfer", "fk_inventory_transfer_source_location",
                "source_location_id", "inventory_location", "id");
        ensureForeignKey("inventory_transfer", "fk_inventory_transfer_destination_warehouse",
                "destination_warehouse_id", "inventory_warehouse", "id");
        ensureForeignKey("inventory_transfer", "fk_inventory_transfer_destination_location",
                "destination_location_id", "inventory_location", "id");
        ensureForeignKey("inventory_transfer_line", "fk_inventory_transfer_line_header",
                "transfer_id", "inventory_transfer", "id");
        ensureForeignKey("inventory_transfer_line", "fk_inventory_transfer_line_source_inventory",
                "source_inventory_id", "inventory", "id");
        ensureForeignKey("inventory_transfer_receipt_operation", "fk_transfer_receipt_operation_header",
                "transfer_id", "inventory_transfer", "id");
        ensureForeignKey("inventory_transfer_receipt_line", "fk_transfer_receipt_line_operation",
                "operation_id", "inventory_transfer_receipt_operation", "id");
        ensureForeignKey("inventory_transfer_receipt_line", "fk_transfer_receipt_line_transfer_line",
                "transfer_line_id", "inventory_transfer_line", "id");
        ensureForeignKey("inventory_transfer_receipt_line", "fk_transfer_receipt_line_source_inventory",
                "source_inventory_id", "inventory", "id");
        ensureForeignKey("inventory_transfer_receipt_line", "fk_transfer_receipt_line_destination_inventory",
                "destination_inventory_id", "inventory", "id");
        ensureForeignKey("inventory_count_session", "fk_inventory_count_session_warehouse",
                "warehouse_id", "inventory_warehouse", "id");
        ensureForeignKey("inventory_count_session", "fk_inventory_count_session_location",
                "location_id", "inventory_location", "id");
        ensureForeignKey("inventory_count_review", "fk_inventory_count_review_session",
                "session_id", "inventory_count_session", "id");
        ensureForeignKey("inventory_count_session_line", "fk_inventory_count_line_session",
                "session_id", "inventory_count_session", "id");
        ensureForeignKey("inventory_count_session_line", "fk_inventory_count_line_inventory",
                "inventory_id", "inventory", "id");
        ensureForeignKey("inventory_count_session_line", "fk_inventory_count_line_location",
                "location_id", "inventory_location", "id");
        ensureForeignKey("inventory_count_submission", "fk_inventory_count_submission_session",
                "session_id", "inventory_count_session", "id");
        ensureForeignKey("inventory_count_observation", "fk_inventory_count_observation_submission",
                "submission_id", "inventory_count_submission", "id");
        ensureForeignKey("inventory_count_observation", "fk_inventory_count_observation_session",
                "session_id", "inventory_count_session", "id");
        ensureForeignKey("inventory_count_observation", "fk_inventory_count_observation_inventory",
                "inventory_id", "inventory", "id");
        ensureForeignKey("inventory_shortage_case", "fk_inventory_shortage_inventory",
                "inventory_id", "inventory", "id");
        ensureForeignKey("inventory_shortage_reallocation", "fk_shortage_reallocation_case",
                "shortage_case_id", "inventory_shortage_case", "id");
        ensureForeignKey("inventory_shortage_reallocation", "fk_shortage_reallocation_inventory",
                "target_inventory_id", "inventory", "id");
        ensureForeignKey("inventory_reservation_line", "fk_inventory_reservation_line_header",
                "reservation_id", "inventory_reservation", "id");
        ensureForeignKey("inventory_reservation_line", "fk_inventory_reservation_line_inventory",
                "inventory_id", "inventory", "id");
        ensureForeignKey("inventory_reservation_operation", "fk_inventory_reservation_operation_header",
                "reservation_id", "inventory_reservation", "id");

        ensureCheckConstraint("inventory", "ck_inventory_balance_non_negative",
                "physical_quantity >= 0 AND reserved_quantity >= 0 AND defective_quantity >= 0 " +
                        "AND pending_quantity >= 0 AND in_transit_quantity >= 0 AND " +
                        "reserved_quantity + defective_quantity + pending_quantity <= physical_quantity");
        ensureCheckConstraint("stock_movement", "ck_stock_movement_balance_non_negative",
                "physical_before >= 0 AND physical_after >= 0 AND reserved_before >= 0 AND reserved_after >= 0 " +
                        "AND (defective_before IS NULL OR defective_before >= 0) " +
                        "AND (defective_after IS NULL OR defective_after >= 0) " +
                        "AND (pending_before IS NULL OR pending_before >= 0) " +
                        "AND (pending_after IS NULL OR pending_after >= 0) " +
                        "AND (in_transit_before IS NULL OR in_transit_before >= 0) " +
                        "AND (in_transit_after IS NULL OR in_transit_after >= 0) " +
                        "AND (inventory_version_before IS NULL OR inventory_version_before >= 0) " +
                        "AND (inventory_version_after IS NULL OR inventory_version_after >= 0)");
        ensureCheckConstraint("inventory_reservation_line", "ck_reservation_line_quantities",
                "requested_quantity > 0 AND dispatched_quantity >= 0 AND released_quantity >= 0 " +
                        "AND dispatched_quantity + released_quantity <= requested_quantity");
        ensureCheckConstraint("inventory_return_line_progress", "ck_return_progress_quantities",
                "approved_quantity > 0 AND received_quantity >= 0 AND received_quantity <= approved_quantity");
    }

    /** Các FK mới được thêm NOT VALID; chỉ validate FK nào dữ liệu hiện tại đã sạch. */
    private void validateForeignKeysWhenClean() {
        validateForeignKeyIfClean("inventory_location", "fk_inventory_location_warehouse", "warehouse_id", "inventory_warehouse", "id");
        validateForeignKeyIfClean("inventory", "fk_inventory_warehouse", "warehouse_id", "inventory_warehouse", "id");
        validateForeignKeyIfClean("inventory_warehouse_access_grant", "fk_inventory_warehouse_access_warehouse", "warehouse_id", "inventory_warehouse", "id");
        validateForeignKeyIfClean("inventory_supplier_receipt", "fk_supplier_receipt_warehouse", "warehouse_id", "inventory_warehouse", "id");
        validateForeignKeyIfClean("inventory_supplier_receipt_line", "fk_supplier_receipt_line_header", "receipt_id", "inventory_supplier_receipt", "id");
        validateForeignKeyIfClean("inventory_supplier_receipt_line", "fk_supplier_receipt_line_location", "location_id", "inventory_location", "id");
        validateForeignKeyIfClean("inventory_supplier_receipt_document", "fk_supplier_receipt_document_header", "receipt_id", "inventory_supplier_receipt", "id");
        validateForeignKeyIfClean("stock_movement", "fk_stock_movement_inventory", "inventory_id", "inventory", "id");
        validateForeignKeyIfClean("inventory_return_receipt", "fk_return_receipt_inventory", "inventory_id", "inventory", "id");
        validateForeignKeyIfClean("inventory_return_inspection", "fk_return_inspection_receipt", "receipt_id", "inventory_return_receipt", "id");
        validateForeignKeyIfClean("inventory_return_inspection", "fk_return_inspection_inventory", "inventory_id", "inventory", "id");
        validateForeignKeyIfClean("inventory_transfer", "fk_inventory_transfer_source_warehouse", "source_warehouse_id", "inventory_warehouse", "id");
        validateForeignKeyIfClean("inventory_transfer", "fk_inventory_transfer_source_location", "source_location_id", "inventory_location", "id");
        validateForeignKeyIfClean("inventory_transfer", "fk_inventory_transfer_destination_warehouse", "destination_warehouse_id", "inventory_warehouse", "id");
        validateForeignKeyIfClean("inventory_transfer", "fk_inventory_transfer_destination_location", "destination_location_id", "inventory_location", "id");
        validateForeignKeyIfClean("inventory_transfer_line", "fk_inventory_transfer_line_header", "transfer_id", "inventory_transfer", "id");
        validateForeignKeyIfClean("inventory_transfer_line", "fk_inventory_transfer_line_source_inventory", "source_inventory_id", "inventory", "id");
        validateForeignKeyIfClean("inventory_transfer_receipt_operation", "fk_transfer_receipt_operation_header", "transfer_id", "inventory_transfer", "id");
        validateForeignKeyIfClean("inventory_transfer_receipt_line", "fk_transfer_receipt_line_operation", "operation_id", "inventory_transfer_receipt_operation", "id");
        validateForeignKeyIfClean("inventory_transfer_receipt_line", "fk_transfer_receipt_line_transfer_line", "transfer_line_id", "inventory_transfer_line", "id");
        validateForeignKeyIfClean("inventory_transfer_receipt_line", "fk_transfer_receipt_line_source_inventory", "source_inventory_id", "inventory", "id");
        validateForeignKeyIfClean("inventory_transfer_receipt_line", "fk_transfer_receipt_line_destination_inventory", "destination_inventory_id", "inventory", "id");
        validateForeignKeyIfClean("inventory_count_session", "fk_inventory_count_session_warehouse", "warehouse_id", "inventory_warehouse", "id");
        validateForeignKeyIfClean("inventory_count_session", "fk_inventory_count_session_location", "location_id", "inventory_location", "id");
        validateForeignKeyIfClean("inventory_count_review", "fk_inventory_count_review_session", "session_id", "inventory_count_session", "id");
        validateForeignKeyIfClean("inventory_count_session_line", "fk_inventory_count_line_session", "session_id", "inventory_count_session", "id");
        validateForeignKeyIfClean("inventory_count_session_line", "fk_inventory_count_line_inventory", "inventory_id", "inventory", "id");
        validateForeignKeyIfClean("inventory_count_session_line", "fk_inventory_count_line_location", "location_id", "inventory_location", "id");
        validateForeignKeyIfClean("inventory_count_submission", "fk_inventory_count_submission_session", "session_id", "inventory_count_session", "id");
        validateForeignKeyIfClean("inventory_count_observation", "fk_inventory_count_observation_submission", "submission_id", "inventory_count_submission", "id");
        validateForeignKeyIfClean("inventory_count_observation", "fk_inventory_count_observation_session", "session_id", "inventory_count_session", "id");
        validateForeignKeyIfClean("inventory_count_observation", "fk_inventory_count_observation_inventory", "inventory_id", "inventory", "id");
        validateForeignKeyIfClean("inventory_shortage_case", "fk_inventory_shortage_inventory", "inventory_id", "inventory", "id");
        validateForeignKeyIfClean("inventory_shortage_reallocation", "fk_shortage_reallocation_case", "shortage_case_id", "inventory_shortage_case", "id");
        validateForeignKeyIfClean("inventory_shortage_reallocation", "fk_shortage_reallocation_inventory", "target_inventory_id", "inventory", "id");
        validateForeignKeyIfClean("inventory_reservation_line", "fk_inventory_reservation_line_header", "reservation_id", "inventory_reservation", "id");
        validateForeignKeyIfClean("inventory_reservation_line", "fk_inventory_reservation_line_inventory", "inventory_id", "inventory", "id");
        validateForeignKeyIfClean("inventory_reservation_operation", "fk_inventory_reservation_operation_header", "reservation_id", "inventory_reservation", "id");
        validateForeignKeyIfClean("inventory_stock_operation", "fk_inventory_stock_operation_inventory",
                "inventory_id", "inventory", "id");
    }

    /** PostgreSQL tự quét dữ liệu đúng một lần khi VALIDATE; lỗi legacy được giữ NOT VALID để đối soát sau. */
    private void validateForeignKeyIfClean(String table, String constraint, String column,
                                           String referencedTable, String referencedColumn) {
        if (!constraintExists(table, constraint) || isValidated(table, constraint)) return;
        try {
            jdbcTemplate.execute("ALTER TABLE " + table + " VALIDATE CONSTRAINT " + constraint);
            log.info("Đã validate FK {}", constraint);
        } catch (DataAccessException invalidLegacyData) {
            // NOT VALID vẫn kiểm tra các bản ghi mới; không sửa/xóa dữ liệu cũ tự động.
            if (!isLegacyConstraintViolation(invalidLegacyData)) throw invalidLegacyData;
            log.warn("FK {} chưa thể validate do dữ liệu lịch sử; giữ NOT VALID để đối soát thủ công: {}",
                    constraint, invalidLegacyData.getMostSpecificCause().getMessage());
        }
    }

    /** Validate CHECK trực tiếp; PostgreSQL báo vi phạm mà không cần chạy thêm một lần quét EXISTS trước đó. */
    private void validateBalanceChecksWhenClean() {
        validateCheckIfClean("inventory", "ck_inventory_balance_non_negative",
                "physical_quantity < 0 OR reserved_quantity < 0 OR defective_quantity < 0 " +
                        "OR pending_quantity < 0 OR in_transit_quantity < 0 OR " +
                        "reserved_quantity + defective_quantity + pending_quantity > physical_quantity");
        validateCheckIfClean("stock_movement", "ck_stock_movement_balance_non_negative",
                "physical_before < 0 OR physical_after < 0 OR reserved_before < 0 OR reserved_after < 0 " +
                        "OR COALESCE(defective_before, 0) < 0 OR COALESCE(defective_after, 0) < 0 " +
                        "OR COALESCE(pending_before, 0) < 0 OR COALESCE(pending_after, 0) < 0 " +
                        "OR COALESCE(in_transit_before, 0) < 0 OR COALESCE(in_transit_after, 0) < 0 " +
                        "OR COALESCE(inventory_version_before, 0) < 0 OR COALESCE(inventory_version_after, 0) < 0");
        validateCheckIfClean("inventory_reservation_line", "ck_reservation_line_quantities",
                "requested_quantity <= 0 OR dispatched_quantity < 0 OR released_quantity < 0 " +
                        "OR dispatched_quantity + released_quantity > requested_quantity");
        validateCheckIfClean("inventory_return_line_progress", "ck_return_progress_quantities",
                "approved_quantity <= 0 OR received_quantity < 0 OR received_quantity > approved_quantity");
    }

    private void validateCheckIfClean(String table, String constraint, String invalidPredicate) {
        if (!constraintExists(table, constraint) || isValidated(table, constraint)) return;
        try {
            jdbcTemplate.execute("ALTER TABLE " + table + " VALIDATE CONSTRAINT " + constraint);
            log.info("Đã validate CHECK {}", constraint);
        } catch (DataAccessException invalidLegacyData) {
            if (!isLegacyConstraintViolation(invalidLegacyData)) throw invalidLegacyData;
            log.warn("CHECK {} chưa thể validate do dữ liệu lịch sử; giữ NOT VALID để đối soát thủ công: {}",
                    constraint, invalidLegacyData.getMostSpecificCause().getMessage());
        }
    }

    /** Chỉ bỏ qua lỗi dữ liệu cũ vi phạm constraint; lỗi kết nối/quyền vẫn phải fail để không che lỗi vận hành. */
    private boolean isLegacyConstraintViolation(Throwable failure) {
        for (Throwable current = failure; current != null; current = current.getCause()) {
            if (current instanceof SQLException sqlException) {
                for (SQLException sql = sqlException; sql != null; sql = sql.getNextException()) {
                    String state = sql.getSQLState();
                    if (state != null && state.startsWith("23")) return true;
                }
            }
        }
        return false;
    }

    private boolean constraintExists(String table, String constraint) {
        Boolean exists = jdbcTemplate.queryForObject("""
                SELECT EXISTS (
                    SELECT 1 FROM pg_constraint
                    WHERE conrelid = to_regclass(?) AND conname = ?
                )
                """, Boolean.class, table, constraint);
        return Boolean.TRUE.equals(exists);
    }

    private boolean isValidated(String table, String constraint) {
        Boolean validated = jdbcTemplate.queryForObject("""
                SELECT COALESCE((SELECT convalidated FROM pg_constraint
                    WHERE conrelid = to_regclass(?) AND conname = ?), FALSE)
                """, Boolean.class, table, constraint);
        return Boolean.TRUE.equals(validated);
    }

    private void ensureCheckConstraint(String table, String constraint, String expression) {
        String sql = "DO $$ BEGIN " +
                "PERFORM pg_advisory_xact_lock(hashtext('inventory-schema-migrations')); " +
                "IF to_regclass('" + table + "') IS NOT NULL AND NOT EXISTS (SELECT 1 FROM pg_constraint " +
                "WHERE conrelid = to_regclass('" + table + "') AND conname = '" + constraint + "') THEN " +
                "ALTER TABLE " + table + " ADD CONSTRAINT " + constraint + " CHECK (" + expression + ") NOT VALID; " +
                "END IF; END $$";
        jdbcTemplate.execute(sql);
    }

    /**
     * Tạo constraint chỉ khi chưa có constraint cùng tên trên bảng hiện tại.
     * Tên bảng/cột chỉ đến từ hằng số nội bộ, không được nhận từ request người dùng.
     */
    private void ensureForeignKey(String table, String constraint, String column,
                                  String referencedTable, String referencedColumn) {
        ensureForeignKey(table, constraint, column, referencedTable, referencedColumn, true);
    }

    /** Thêm FK mới mà chưa quét lịch sử đang chứa warehouseId từ mô hình cũ bên ngoài. */
    private void ensureForeignKeyNotValid(String table, String constraint, String column,
                                          String referencedTable, String referencedColumn) {
        ensureForeignKey(table, constraint, column, referencedTable, referencedColumn, true);
    }

    private void ensureForeignKey(String table, String constraint, String column,
                                  String referencedTable, String referencedColumn, boolean notValid) {
        String sql = "DO $$ BEGIN " +
                // Khóa theo database để nhiều instance khởi động cùng lúc không cùng tạo một constraint.
                "PERFORM pg_advisory_xact_lock(hashtext('inventory-schema-fk-initializer')); " +
                "IF to_regclass('" + table + "') IS NOT NULL AND to_regclass('" + referencedTable +
                "') IS NOT NULL AND NOT EXISTS (SELECT 1 FROM pg_constraint " +
                "WHERE conrelid = '" + table + "'::regclass AND conname = '" + constraint + "') THEN " +
                "ALTER TABLE " + table + " ADD CONSTRAINT " + constraint +
                " FOREIGN KEY (" + column + ") REFERENCES " + referencedTable + " (" + referencedColumn + ")" +
                (notValid ? " NOT VALID" : "") + "; " +
                "END IF; END $$";
        jdbcTemplate.execute(sql);
    }
}
