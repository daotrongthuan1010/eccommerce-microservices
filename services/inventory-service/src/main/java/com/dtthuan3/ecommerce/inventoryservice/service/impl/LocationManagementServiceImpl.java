package com.dtthuan3.ecommerce.inventoryservice.service.impl;

import com.dtthuan3.ecommerce.common.exception.BusinessException;
import com.dtthuan3.ecommerce.common.exception.ErrorCode;
import com.dtthuan3.ecommerce.common.exception.ResourceNotFoundException;
import com.dtthuan3.ecommerce.inventoryservice.dto.request.CreateLocationRequest;
import com.dtthuan3.ecommerce.inventoryservice.dto.request.UpdateLocationRequest;
import com.dtthuan3.ecommerce.inventoryservice.dto.response.LocationResponse;
import com.dtthuan3.ecommerce.inventoryservice.entity.InventoryLocation;
import com.dtthuan3.ecommerce.inventoryservice.mapper.InventoryLocationMapper;
import com.dtthuan3.ecommerce.inventoryservice.repository.InventoryLocationRepository;
import com.dtthuan3.ecommerce.inventoryservice.service.LocationManagementService;
import com.dtthuan3.ecommerce.inventoryservice.service.InventoryLocationAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** Xử lý nghiệp vụ Location và đồng bộ cache sau khi dữ liệu PostgreSQL thay đổi. */
@Service
@RequiredArgsConstructor
public class LocationManagementServiceImpl implements LocationManagementService {

    private final InventoryLocationRepository locationRepository;
    private final InventoryLocationMapper locationMapper;
    private final InventoryLocationAccessService locationAccessService;

    /** Tạo Location và xóa cache đọc sau khi dữ liệu mới được ghi thành công. */
    @Override
    @Transactional
    @CacheEvict(cacheNames = "inventory-location-queries", allEntries = true)
    public LocationResponse createLocation(CreateLocationRequest request) {
        String warehouseId = request.warehouseId().trim();
        // Nếu Warehouse projection đã được bật, chỉ cho tạo Location trong kho đang hoạt động.
        locationAccessService.requireActiveWarehouse(warehouseId, null);
        String code = request.code().trim().toUpperCase(Locale.ROOT);
        InventoryLocation location = InventoryLocation.builder()
                .id(request.id() == null || request.id().isBlank() ? UUID.randomUUID().toString() : request.id().trim())
                .warehouseId(warehouseId)
                .code(code)
                .name(request.name().trim())
                .active(true)
                .build();
        try {
            return locationMapper.toResponse(locationRepository.saveAndFlush(location));
        } catch (DataIntegrityViolationException ex) {
            // DB unique constraint là chốt cuối chống hai request đồng thời tạo cùng ID/code.
            throw new BusinessException(ErrorCode.CONFLICT,
                    "ID hoặc mã Location đã tồn tại trong Warehouse này");
        }
    }

    /** Cache kết quả theo ID; sync=true hạn chế nhiều request đồng thời cùng truy vấn khi cache-miss. */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "inventory-location-queries", key = "'id:' + #locationId", sync = true)
    public LocationResponse getLocation(String locationId) {
        InventoryLocation location = locationRepository.findById(locationId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Location với ID: " + locationId));
        return locationMapper.toResponse(location);
    }

    /** Cache danh sách theo warehouseId; tiền tố key tách với key tra cứu theo Location ID. */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "inventory-location-queries", key = "'warehouse:' + #warehouseId", sync = true)
    public List<LocationResponse> listLocations(String warehouseId) {
        if (warehouseId == null || warehouseId.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "warehouseId là bắt buộc");
        }
        return locationRepository.findAllByWarehouseIdOrderByCode(warehouseId.trim()).stream()
                .map(locationMapper::toResponse)
                .toList();
    }

    /** Cập nhật code/name; làm mất hiệu lực cache theo ID và theo Warehouse. */
    @Override
    @Transactional
    @CacheEvict(cacheNames = "inventory-location-queries", allEntries = true)
    public LocationResponse updateLocation(String locationId, UpdateLocationRequest request) {
        InventoryLocation location = locationRepository.findById(locationId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Location với ID: " + locationId));
        location.setCode(request.code().trim().toUpperCase(Locale.ROOT));
        location.setName(request.name().trim());
        try {
            // Flush ngay trong try để bắt unique constraint tại đây và đổi lỗi DB thành HTTP conflict chuẩn.
            return locationMapper.toResponse(locationRepository.saveAndFlush(location));
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessException(ErrorCode.CONFLICT,
                    "Mã Location đã được sử dụng trong Warehouse này");
        }
    }

    /** Tắt mềm Location để giữ lịch sử; xóa cache sau khi cập nhật trạng thái. */
    @Override
    @Transactional
    @CacheEvict(cacheNames = "inventory-location-queries", allEntries = true)
    public LocationResponse setLocationActive(String locationId, boolean active) {
        InventoryLocation location = locationRepository.findById(locationId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Location với ID: " + locationId));
        if (active) {
            // Không cho kích hoạt Location nếu Warehouse đã có projection nhưng đang ngừng hoạt động.
            locationAccessService.requireActiveWarehouse(location.getWarehouseId(), null);
        }
        location.setActive(active);
        return locationMapper.toResponse(locationRepository.save(location));
    }
}
