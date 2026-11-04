package com.dtthuan3.ecommerce.inventoryservice.service;

import com.dtthuan3.ecommerce.inventoryservice.dto.request.CreateWarehouseRequest;
import com.dtthuan3.ecommerce.inventoryservice.dto.request.UpdateWarehouseRequest;
import com.dtthuan3.ecommerce.inventoryservice.dto.response.WarehouseResponse;
import com.dtthuan3.ecommerce.inventoryservice.dto.response.WarehouseCursorPageResponse;
import com.dtthuan3.ecommerce.common.response.PageResponse;

import java.util.List;

/**
 * Hợp đồng nghiệp vụ quản lý Warehouse nội bộ Inventory; controller phụ thuộc interface này.
 * Interface này công bố các use case Inventory cho controller, worker và adapter gọi tới. Class triển khai
 * nằm ở service/impl, vì vậy bên gọi phụ thuộc vào hợp đồng thay vì chi tiết xử lý.
 */
public interface WarehouseManagementService {
    /** Tạo kho ACTIVE và sinh UUID nếu request không cung cấp ID. */
    WarehouseResponse create(CreateWarehouseRequest request);

    /** Đọc chi tiết một kho theo ID. */
    WarehouseResponse getById(String warehouseId);

    /** Liệt kê các kho thuộc một enterprise. */
    List<WarehouseResponse> listByEnterprise(String enterpriseId);

    /** Liệt kê một trang kho để tránh tải toàn bộ danh sách lớn vào bộ nhớ. */
    PageResponse<WarehouseResponse> listPageByEnterprise(String enterpriseId, int page, int size);

    /** Cursor pagination cho danh sách rất lớn; không cần COUNT(*) hoặc OFFSET sâu. */
    WarehouseCursorPageResponse listCursorByEnterprise(
            String enterpriseId, String afterCode, String afterId, int size);

    /** Sửa mã/tên/địa chỉ nhưng giữ nguyên enterprise và trạng thái. */
    WarehouseResponse update(String warehouseId, UpdateWarehouseRequest request);

    /** Đổi ACTIVE/INACTIVE mà không xóa kho khỏi các dữ liệu lịch sử. */
    WarehouseResponse setActive(String warehouseId, boolean active);
}
