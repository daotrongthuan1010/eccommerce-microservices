package com.dtthuan3.ecommerce.inventoryservice.repository;

import com.dtthuan3.ecommerce.inventoryservice.entity.WarehouseReferenceProjection;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface WarehouseReferenceProjectionRepository extends JpaRepository<WarehouseReferenceProjection, String> {
    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("select w from WarehouseReferenceProjection w where w.warehouseId = :id")
    Optional<WarehouseReferenceProjection> findByWarehouseIdForRead(@Param("id") String warehouseId);

    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("select w from WarehouseReferenceProjection w where w.warehouseId in :ids order by w.warehouseId")
    List<WarehouseReferenceProjection> findAllByWarehouseIdInForRead(@Param("ids") Collection<String> warehouseIds);
}
