package com.dtthuan3.ecommerce.inventoryservice.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Cổng đọc và khóa các bản ghi kho/vị trí trước thao tác ghi tồn.
 * Service chỉ phụ thuộc interface này; adapter JDBC bên dưới thực hiện câu SQL PostgreSQL cụ thể.
 */
public interface InventoryLocationAccessRepository {
    /** Đọc và khóa SHARE các Location theo ID đã sắp xếp để giảm tranh chấp deadlock. */
    List<LocationSnapshot> findLocationsForShare(Collection<String> locationIds);

    /** Đọc và khóa SHARE các Warehouse theo ID đã sắp xếp trong cùng transaction. */
    List<WarehouseSnapshot> findWarehousesForShare(Collection<String> warehouseIds);

    /** Đọc và khóa SHARE một Warehouse khi workflow cần xác thực kho bắt buộc tồn tại. */
    Optional<WarehouseSnapshot> findWarehouseForShare(String warehouseId);

    /** Dữ liệu tối thiểu của Location cần để kiểm tra warehouse ownership và trạng thái hoạt động. */
    record LocationSnapshot(String id, String warehouseId, boolean active) {}

    /** Dữ liệu Warehouse tối thiểu dùng để xác minh trạng thái kho và enterprise sở hữu. */
    record WarehouseSnapshot(String warehouseId, String enterpriseId, String status) {}
}
