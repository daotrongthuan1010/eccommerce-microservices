package com.dtthuan3.ecommerce.inventoryservice.repository;

import com.dtthuan3.ecommerce.inventoryservice.entity.InventoryLocation;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

/** Truy vấn Location nội bộ; warehouseId là scalar reference, không phải JPA relation. */
public interface InventoryLocationRepository extends JpaRepository<InventoryLocation, String> {

    /**
     * Tải và khóa các Location theo thứ tự ID để request batch có thứ tự khóa ổn định,
     * giảm nguy cơ deadlock khi nhiều transaction cùng kiểm tra nhiều Location.
     */
    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("select l from InventoryLocation l where l.id in :ids order by l.id")
    List<InventoryLocation> findAllByIdInForRead(@Param("ids") Collection<String> ids);

    /** Spring Data JPA tạo truy vấn theo Warehouse và sắp xếp kết quả bằng code. */
    List<InventoryLocation> findAllByWarehouseIdOrderByCode(String warehouseId);
}
