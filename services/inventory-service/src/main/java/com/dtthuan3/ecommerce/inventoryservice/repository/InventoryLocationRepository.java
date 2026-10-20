package com.dtthuan3.ecommerce.inventoryservice.repository;

import com.dtthuan3.ecommerce.inventoryservice.entity.InventoryLocation;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/** Truy vấn Location nội bộ; warehouseId là scalar reference, không phải JPA relation. */
public interface InventoryLocationRepository extends JpaRepository<InventoryLocation, String> {

    /** Truy vấn phân trang theo Warehouse; dùng cho API danh sách có lượng dữ liệu lớn. */
    Page<InventoryLocation> findAllByWarehouseIdOrderByCode(String warehouseId, Pageable pageable);
}
