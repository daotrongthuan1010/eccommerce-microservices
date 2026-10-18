package com.dtthuan3.ecommerce.inventoryservice.service;

import com.dtthuan3.ecommerce.inventoryservice.dto.request.CreateWarehouseRequest;
import com.dtthuan3.ecommerce.inventoryservice.dto.request.UpdateWarehouseRequest;
import com.dtthuan3.ecommerce.inventoryservice.dto.response.WarehouseResponse;

import java.util.List;

/** Hợp đồng nghiệp vụ quản lý Warehouse nội bộ Inventory; controller phụ thuộc interface này. */
public interface WarehouseManagementService {
    /** Tạo kho ACTIVE và sinh UUID nếu request không cung cấp ID. */
    WarehouseResponse create(CreateWarehouseRequest request);

    /** Đọc chi tiết một kho theo ID. */
    WarehouseResponse getById(String warehouseId);

    /** Liệt kê các kho thuộc một enterprise. */
    List<WarehouseResponse> listByEnterprise(String enterpriseId);

    /** Sửa mã/tên/địa chỉ nhưng giữ nguyên enterprise và trạng thái. */
    WarehouseResponse update(String warehouseId, UpdateWarehouseRequest request);

    /** Đổi ACTIVE/INACTIVE mà không xóa kho khỏi các dữ liệu lịch sử. */
    WarehouseResponse setActive(String warehouseId, boolean active);
}
