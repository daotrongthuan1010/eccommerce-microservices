package com.dtthuan3.ecommerce.inventoryservice.service;

import com.dtthuan3.ecommerce.inventoryservice.dto.request.CreateLocationRequest;
import com.dtthuan3.ecommerce.inventoryservice.dto.request.UpdateLocationRequest;
import com.dtthuan3.ecommerce.inventoryservice.dto.response.LocationResponse;

import java.util.List;

/** Hợp đồng nghiệp vụ cho quản lý Location thuộc Inventory. */
public interface LocationManagementService {
    /** Tạo Location mới trong Warehouse đã được cung cấp. */
    LocationResponse createLocation(CreateLocationRequest request);

    /** Đọc một Location theo ID. */
    LocationResponse getLocation(String locationId);

    /** Lấy danh sách Location thuộc một Warehouse. */
    List<LocationResponse> listLocations(String warehouseId);

    /** Đổi code/name, không đổi Warehouse sở hữu. */
    LocationResponse updateLocation(String locationId, UpdateLocationRequest request);

    /** Bật/tắt Location mà không xóa dữ liệu lịch sử. */
    LocationResponse setLocationActive(String locationId, boolean active);
}
