package com.dtthuan3.ecommerce.inventoryservice.service.impl;

import com.dtthuan3.ecommerce.inventoryservice.dto.cache.LocationPageSnapshot;
import com.dtthuan3.ecommerce.inventoryservice.mapper.InventoryLocationMapper;
import com.dtthuan3.ecommerce.inventoryservice.repository.InventoryLocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Đọc và cache từng trang Location theo Warehouse; mọi thay đổi xóa cache qua transactional outbox. */
@Service
@RequiredArgsConstructor
public class InventoryLocationReadCache {
    private final InventoryLocationRepository locationRepository;
    private final InventoryLocationMapper locationMapper;

    /** Key tách theo warehouse/page/size để cache có giới hạn và không lưu PageImpl phụ thuộc framework. */
    @Cacheable(cacheNames = "inventory-location-list-v1",
            key = "'warehouse:' + #warehouseId + ':page:' + #pageable.pageNumber + ':size:' + #pageable.pageSize",
            sync = true)
    @Transactional(readOnly = true)
    public LocationPageSnapshot findPage(String warehouseId, Pageable pageable) {
        var page = locationRepository.findAllByWarehouseIdOrderByCode(warehouseId, pageable);
        return new LocationPageSnapshot(page.getContent().stream().map(locationMapper::toResponse).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements());
    }
}
