package com.dtthuan3.ecommerce.inventoryservice.service.impl;

import com.dtthuan3.ecommerce.common.exception.BusinessException;
import com.dtthuan3.ecommerce.common.exception.ErrorCode;
import com.dtthuan3.ecommerce.inventoryservice.dto.request.CreateLocationRequest;
import com.dtthuan3.ecommerce.inventoryservice.dto.request.UpdateLocationRequest;
import com.dtthuan3.ecommerce.inventoryservice.dto.cache.LocationPageSnapshot;
import com.dtthuan3.ecommerce.inventoryservice.dto.response.LocationResponse;
import com.dtthuan3.ecommerce.inventoryservice.entity.InventoryLocation;
import com.dtthuan3.ecommerce.inventoryservice.mapper.InventoryLocationMapper;
import com.dtthuan3.ecommerce.inventoryservice.repository.InventoryLocationRepository;
import com.dtthuan3.ecommerce.inventoryservice.repository.CacheInvalidationOutboxRepository;
import com.dtthuan3.ecommerce.inventoryservice.entity.CacheInvalidationOutboxEvent;
import com.dtthuan3.ecommerce.inventoryservice.service.LocationManagementService;
import com.dtthuan3.ecommerce.inventoryservice.service.InventoryLocationAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Locale;
import java.util.UUID;

/** Xử lý nghiệp vụ Location và đồng bộ cache sau khi dữ liệu PostgreSQL thay đổi. */
@Service
@RequiredArgsConstructor
public class LocationManagementServiceImpl implements LocationManagementService {

    private final InventoryLocationRepository locationRepository;
    private final InventoryLocationMapper locationMapper;
    private final InventoryLocationAccessService locationAccessService;
    private final CacheInvalidationOutboxRepository invalidationOutboxRepository;
    private final InventoryLocationReadCache locationReadCache;

    /** Tạo Location và xóa cache đọc sau khi dữ liệu mới được ghi thành công. */
    @Override
    @Transactional
    public LocationResponse createLocation(CreateLocationRequest request) {
        String warehouseId = request.warehouseId().trim();
        // Location chỉ được tạo trong Warehouse nội bộ đang hoạt động.
        locationAccessService.requireActiveWarehouse(warehouseId, null);
        String code = request.code().trim().toUpperCase(Locale.ROOT);
        InventoryLocation location = InventoryLocation.builder()
                .id(request.id() == null || request.id().isBlank() ? UUID.randomUUID().toString() : request.id().trim())
                .warehouseId(warehouseId)
                .code(code)
                .name(request.name().trim())
                .active(true)
                .build();
        LocationResponse created;
        try {
            created = locationMapper.toResponse(locationRepository.saveAndFlush(location));
        } catch (DataIntegrityViolationException ex) {
            // DB unique constraint là chốt cuối chống hai request đồng thời tạo cùng ID/code.
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE,
                    "ID hoặc mã Location đã tồn tại trong Warehouse này");
        }
        scheduleLocationListInvalidation(warehouseId);
        return created;
    }

    /** Cache kết quả theo ID; sync=true hạn chế nhiều request đồng thời cùng truy vấn khi cache-miss. */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "inventory-location-by-id-v2", key = "'id:' + #locationId", sync = true)
    public LocationResponse getLocation(String locationId) {
        InventoryLocation location = locationRepository.findById(locationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Không tìm thấy Location với ID: " + locationId));
        return locationMapper.toResponse(location);
    }

    /** Cache từng trang Location; mutation xóa toàn bộ cache danh sách qua outbox sau commit. */
    @Override
    @Transactional(readOnly = true)
    public Page<LocationResponse> listLocations(String warehouseId, Pageable pageable) {
        if (warehouseId == null || warehouseId.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "warehouseId là bắt buộc");
        }
        String normalizedWarehouseId = warehouseId.trim();
        LocationPageSnapshot snapshot = locationReadCache.findPage(normalizedWarehouseId, pageable);
        return new PageImpl<>(snapshot.content(), pageable, snapshot.totalElements());
    }

    /** Cập nhật code/name; làm mất hiệu lực cache theo ID và theo Warehouse. */
    @Override
    @Transactional
    public LocationResponse updateLocation(String locationId, UpdateLocationRequest request) {
        InventoryLocation location = locationRepository.findById(locationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Không tìm thấy Location với ID: " + locationId));
        location.setCode(request.code().trim().toUpperCase(Locale.ROOT));
        location.setName(request.name().trim());
        try {
            // Flush ngay trong try để bắt unique constraint tại đây và đổi lỗi DB thành HTTP conflict chuẩn.
            LocationResponse updated = locationMapper.toResponse(locationRepository.saveAndFlush(location));
            scheduleLocationInvalidation(locationId, location.getWarehouseId());
            return updated;
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE,
                    "Mã Location đã được sử dụng trong Warehouse này");
        }
    }

    /** Tắt mềm Location để giữ lịch sử; xóa cache sau khi cập nhật trạng thái. */
    @Override
    @Transactional
    public LocationResponse setLocationActive(String locationId, boolean active) {
        InventoryLocation location = locationRepository.findById(locationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Không tìm thấy Location với ID: " + locationId));
        if (active) {
            // Không cho kích hoạt Location nếu Warehouse đang ngừng hoạt động.
            locationAccessService.requireActiveWarehouse(location.getWarehouseId(), null);
        }
        location.setActive(active);
        LocationResponse updated = locationMapper.toResponse(locationRepository.save(location));
        scheduleLocationInvalidation(locationId, location.getWarehouseId());
        return updated;
    }

    private void scheduleLocationInvalidation(String locationId, String warehouseId) {
        invalidationOutboxRepository.save(new CacheInvalidationOutboxEvent(
                "inventory-location-by-id-v2", "id:" + locationId));
        scheduleLocationListInvalidation(warehouseId);
    }

    /** Page cache có nhiều key phân trang nên xóa riêng cache danh sách sau mutation. */
    private void scheduleLocationListInvalidation(String warehouseId) {
        invalidationOutboxRepository.save(new CacheInvalidationOutboxEvent(
                "inventory-location-list-v1", "*"));
    }
}
