package com.dtthuan3.ecommerce.inventoryservice.repository;

import java.time.Instant;
import java.util.List;

/** Cổng lưu quyền theo kho; JDBC adapter thực thi insert/delete có khóa duy nhất. */
public interface InventoryWarehouseAccessGrantRepository {
    boolean grant(String warehouseId, String subjectId, String grantedBy, Instant grantedAt);
    boolean revoke(String warehouseId, String subjectId);
    boolean hasAccess(String warehouseId, String subjectId);
    List<WarehouseAccessGrantRecord> listByWarehouse(String warehouseId);

    /** Projection tối giản để API quản trị quyền không trả entity persistence. */
    record WarehouseAccessGrantRecord(String warehouseId, String subjectId, String grantedBy, Instant grantedAt) {}
}
