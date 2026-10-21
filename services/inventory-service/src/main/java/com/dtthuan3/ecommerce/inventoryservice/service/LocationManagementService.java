package com.dtthuan3.ecommerce.inventoryservice.service;

import com.dtthuan3.ecommerce.inventoryservice.dto.request.CreateLocationRequest;
import com.dtthuan3.ecommerce.inventoryservice.dto.request.UpdateLocationRequest;
import com.dtthuan3.ecommerce.inventoryservice.dto.response.LocationResponse;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/** Hợp đồng nghiệp vụ cho quản lý Location thuộc Inventory. */
public interface LocationManagementService {
    /** Tạo Location mới trong Warehouse nội bộ đang hoạt động. */
    LocationResponse createLocation(CreateLocationRequest request);

    /** Đọc một Location theo ID. */
    LocationResponse getLocation(String locationId);

    /** Lấy một trang Location để giới hạn bộ nhớ và payload theo Warehouse. */
    Page<LocationResponse> listLocations(String warehouseId, Pageable pageable);

    /** Đổi code/name, không đổi Warehouse sở hữu. */
    LocationResponse updateLocation(String locationId, UpdateLocationRequest request);

    /** Bật/tắt Location mà không xóa dữ liệu lịch sử. */
    LocationResponse setLocationActive(String locationId, boolean active);
}
