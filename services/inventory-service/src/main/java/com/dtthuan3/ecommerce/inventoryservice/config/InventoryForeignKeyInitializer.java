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
        ensureForeignKey("inventory_supplier_receipt_line", "fk_supplier_receipt_line_header",
                "receipt_id", "inventory_supplier_receipt", "id");
        ensureForeignKey("inventory_supplier_receipt_document", "fk_supplier_receipt_document_header",
                "receipt_id", "inventory_supplier_receipt", "id");
        ensureForeignKey("stock_movement", "fk_stock_movement_inventory",
                "inventory_id", "inventory", "id");
    }

    /**
     * Tạo constraint chỉ khi chưa có constraint cùng tên trên bảng hiện tại.
     * Tên bảng/cột chỉ đến từ hằng số nội bộ, không được nhận từ request người dùng.
     */
    private void ensureForeignKey(String table, String constraint, String column,
                                  String referencedTable, String referencedColumn) {
        String sql = "DO $$ BEGIN " +
                // Khóa theo database để nhiều instance khởi động cùng lúc không cùng tạo một constraint.
                "PERFORM pg_advisory_xact_lock(hashtext('inventory-schema-fk-initializer')); " +
                "IF NOT EXISTS (SELECT 1 FROM pg_constraint " +
                "WHERE conrelid = '" + table + "'::regclass AND conname = '" + constraint + "') THEN " +
                "ALTER TABLE " + table + " ADD CONSTRAINT " + constraint +
                " FOREIGN KEY (" + column + ") REFERENCES " + referencedTable + " (" + referencedColumn + "); " +
                "END IF; END $$";
        jdbcTemplate.execute(sql);
    }
}
