package com.dtthuan3.ecommerce.inventoryservice.config;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Bổ sung các khóa ngoại ở database sau khi Hibernate đã tạo/cập nhật bảng.
 * Entity chỉ giữ các ID dạng String; không cần khai báo quan hệ JPA để database kiểm tra tham chiếu.
 */
@Component
@DependsOn("entityManagerFactory")
public class InventoryForeignKeyInitializer implements InitializingBean {
    private final JdbcTemplate jdbcTemplate;

    public InventoryForeignKeyInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** Chạy trước khi ứng dụng sẵn sàng nhận request và an toàn khi khởi động lại nhiều lần. */
    @Override
    public void afterPropertiesSet() {
        ensureForeignKeyNotValid("inventory_location", "fk_inventory_location_warehouse",
                "warehouse_id", "inventory_warehouse", "id");
        ensureForeignKeyNotValid("inventory", "fk_inventory_warehouse",
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
    }

    /**
     * Tạo constraint chỉ khi chưa có constraint cùng tên trên bảng hiện tại.
     * Tên bảng/cột chỉ đến từ hằng số nội bộ, không được nhận từ request người dùng.
     */
    private void ensureForeignKey(String table, String constraint, String column,
                                  String referencedTable, String referencedColumn) {
        ensureForeignKey(table, constraint, column, referencedTable, referencedColumn, false);
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
                "IF NOT EXISTS (SELECT 1 FROM pg_constraint " +
                "WHERE conrelid = '" + table + "'::regclass AND conname = '" + constraint + "') THEN " +
                "ALTER TABLE " + table + " ADD CONSTRAINT " + constraint +
                " FOREIGN KEY (" + column + ") REFERENCES " + referencedTable + " (" + referencedColumn + ")" +
                (notValid ? " NOT VALID" : "") + "; " +
                "END IF; END $$";
        jdbcTemplate.execute(sql);
    }
}
