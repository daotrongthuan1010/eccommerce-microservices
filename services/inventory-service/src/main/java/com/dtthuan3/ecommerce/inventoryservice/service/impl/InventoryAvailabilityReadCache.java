package com.dtthuan3.ecommerce.inventoryservice.service.impl;

import com.dtthuan3.ecommerce.common.exception.BusinessException;
import com.dtthuan3.ecommerce.common.exception.ErrorCode;
import com.dtthuan3.ecommerce.inventoryservice.dto.cache.InventoryAvailabilityPageSnapshot;
import com.dtthuan3.ecommerce.inventoryservice.dto.response.InventoryResponse;
import com.dtthuan3.ecommerce.inventoryservice.entity.Inventory;
import com.dtthuan3.ecommerce.inventoryservice.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;

import java.math.BigDecimal;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/** Cache lượt tra cứu tồn theo SKU cho trang sản phẩm; dữ liệu chỉ mang tính hiển thị tức thời. */
@Service
@RequiredArgsConstructor
public class InventoryAvailabilityReadCache {
    private final InventoryRepository inventoryRepository;
    private final MeterRegistry meterRegistry;

    // Mặc định để dành 1 kết nối trong pool Hikari 5 kết nối cho các nghiệp vụ ghi/đọc khác.
    @Value("${inventory.cache.availability-db-max-concurrency:4}")
    private int maxDatabaseConcurrency;

    @Value("${inventory.cache.availability-db-wait-ms:100}")
    private long databaseWaitMillis;

    private Semaphore databaseReadPermits;

    /** Tạo bộ giới hạn theo từng instance để cache miss không chiếm hết pool PostgreSQL. */
    @PostConstruct
    void initializeReadLimiter() {
        if (maxDatabaseConcurrency < 1 || databaseWaitMillis < 0) {
            throw new IllegalStateException("Cấu hình giới hạn đọc tồn khả dụng không hợp lệ");
        }
        databaseReadPermits = new Semaphore(maxDatabaseConcurrency);
        meterRegistry.gauge("inventory.availability.db.read.permits.available", databaseReadPermits,
                Semaphore::availablePermits);
    }

    /**
     * Cache chỉ truy vấn có SKU cụ thể để tránh tạo quá nhiều key cho các bộ lọc tùy ý.
     * Semaphore giới hạn số cache miss cùng lúc trong một instance; sync=true không phải khóa phân tán.
     */
    @Cacheable(cacheNames = "inventory-availability-by-sku-v1",
            key = "'sku:' + #sku + ':warehouse:' + #warehouseId + ':location:' + #locationId"
                    + " + ':page:' + #pageable.pageNumber + ':size:' + #pageable.pageSize + ':sort:' + #pageable.sort",
            condition = "#sku != null && !#sku.isBlank()",
            sync = true)
    @Transactional(readOnly = true)
    public InventoryAvailabilityPageSnapshot findAvailable(String sku, String warehouseId,
                                                            String locationId, Pageable pageable) {
        boolean acquired;
        try {
            acquired = databaseReadPermits.tryAcquire(databaseWaitMillis, TimeUnit.MILLISECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            meterRegistry.counter("inventory.availability.db.read.rejected", "reason", "interrupted").increment();
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE,
                    "Hệ thống đang xử lý nhiều yêu cầu tồn kho, vui lòng thử lại sau");
        }
        if (!acquired) {
            meterRegistry.counter("inventory.availability.db.read.rejected", "reason", "capacity").increment();
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE,
                    "Hệ thống đang xử lý nhiều yêu cầu tồn kho, vui lòng thử lại sau");
        }

        try {
            var page = inventoryRepository.findAvailable(sku, warehouseId, locationId, pageable);
            return new InventoryAvailabilityPageSnapshot(page.getContent().stream()
                    .map(this::toResponse).toList(), page.getTotalElements());
        } finally {
            databaseReadPermits.release();
        }
    }

    /** Ánh xạ lượng có thể bán để response từ Redis giống response đọc trực tiếp database. */
    private InventoryResponse toResponse(Inventory inventory) {
        BigDecimal availableQuantity = inventory.getPhysicalQuantity()
                .subtract(inventory.getReservedQuantity())
                .subtract(inventory.getDefectiveQuantity())
                .subtract(inventory.getPendingQuantity());
        return new InventoryResponse(inventory.getId(), inventory.getEnterpriseId(), inventory.getWarehouseId(),
                inventory.getLocationId(), inventory.getSku(), inventory.getPhysicalQuantity(),
                inventory.getReservedQuantity(), inventory.getDefectiveQuantity(), inventory.getPendingQuantity(),
                inventory.getInTransitQuantity(), availableQuantity, inventory.getVersion(),
                inventory.getCreatedAt(), inventory.getUpdatedAt());
    }
}
