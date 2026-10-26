package com.dtthuan3.ecommerce.inventoryservice.service.impl;

import com.dtthuan3.ecommerce.common.exception.BusinessException;
import com.dtthuan3.ecommerce.common.exception.ErrorCode;
import com.dtthuan3.ecommerce.common.exception.ResourceNotFoundException;
import com.dtthuan3.ecommerce.inventoryservice.dto.request.CreateWarehouseRequest;
import com.dtthuan3.ecommerce.inventoryservice.dto.request.UpdateWarehouseRequest;
import com.dtthuan3.ecommerce.inventoryservice.dto.response.WarehouseResponse;
import com.dtthuan3.ecommerce.inventoryservice.entity.CacheInvalidationOutboxEvent;
import com.dtthuan3.ecommerce.inventoryservice.repository.CacheInvalidationOutboxRepository;
import com.dtthuan3.ecommerce.inventoryservice.repository.WarehouseRepository;
import com.dtthuan3.ecommerce.inventoryservice.repository.model.WarehouseRecord;
import com.dtthuan3.ecommerce.inventoryservice.service.WarehouseManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** Xử lý Warehouse bằng các bước ngắn; SQL chi tiết được giữ trong repository JDBC. */
@Service
@RequiredArgsConstructor
public class WarehouseManagementServiceImpl implements WarehouseManagementService {
    private final WarehouseRepository warehouseRepository;
    private final CacheInvalidationOutboxRepository invalidationOutboxRepository;

    /** Chuẩn hóa dữ liệu trước khi insert; database unique constraint xử lý tranh chấp đồng thời. */
    @Override
    @Transactional
    public WarehouseResponse create(CreateWarehouseRequest request) {
        Instant now = Instant.now();
        WarehouseRecord warehouse = new WarehouseRecord(request.id() == null || request.id().isBlank()
                ? UUID.randomUUID().toString() : request.id().trim(),
                request.enterpriseId().trim(), normalizeCode(request.code()), request.name().trim(),
                blankToNull(request.address()), "ACTIVE", now, now);
        try {
            warehouseRepository.insert(warehouse);
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE,
                    "ID Warehouse hoặc mã Warehouse đã tồn tại trong enterprise này");
        }
        scheduleWarehouseInvalidation(warehouse.id(), warehouse.enterpriseId());
        return toResponse(warehouse);
    }

    /** Tìm theo ID và đổi kết quả JDBC thành DTO; không trả bản ghi persistence ra ngoài. */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "inventory-warehouse-by-id-v1", key = "'id:' + #warehouseId", sync = true)
    public WarehouseResponse getById(String warehouseId) {
        return warehouseRepository.findById(warehouseId)
                .map(this::toResponse)
                .orElseThrow(() -> notFound(warehouseId));
    }

    /** Không trả toàn bộ kho của hệ thống; truy vấn luôn giới hạn theo enterprise được yêu cầu. */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "inventory-warehouse-list-by-enterprise-v1",
            key = "#enterpriseId == null ? 'enterprise:' : 'enterprise:' + #enterpriseId.trim()", sync = true)
    public List<WarehouseResponse> listByEnterprise(String enterpriseId) {
        if (enterpriseId == null || enterpriseId.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "enterpriseId là bắt buộc");
        }
        return warehouseRepository.findByEnterpriseId(enterpriseId.trim()).stream()
                .map(this::toResponse).toList();
    }

    /** Giữ nguyên owner/status, chỉ sửa thông tin hiển thị và ánh xạ mã trùng thành HTTP conflict. */
    @Override
    @Transactional
    public WarehouseResponse update(String warehouseId, UpdateWarehouseRequest request) {
        WarehouseRecord current = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> notFound(warehouseId));
        WarehouseRecord updated = new WarehouseRecord(current.id(), current.enterpriseId(),
                normalizeCode(request.code()), request.name().trim(), blankToNull(request.address()),
                current.status(), current.createdAt(), Instant.now());
        try {
            warehouseRepository.updateDetails(warehouseId, updated.code(), updated.name(),
                    updated.address(), updated.updatedAt());
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE,
                    "Mã Warehouse đã được sử dụng trong enterprise này");
        }
        scheduleWarehouseInvalidation(warehouseId, current.enterpriseId());
        return toResponse(updated);
    }

    /** Khóa dòng Warehouse trước khi đổi trạng thái để không đua với giao dịch đang dùng kho. */
    @Override
    @Transactional
    public WarehouseResponse setActive(String warehouseId, boolean active) {
        WarehouseRecord current = warehouseRepository.findByIdForShare(warehouseId)
                .orElseThrow(() -> notFound(warehouseId));
        String status = active ? "ACTIVE" : "INACTIVE";
        Instant updatedAt = Instant.now();
        warehouseRepository.updateStatus(warehouseId, status, updatedAt);
        scheduleWarehouseInvalidation(warehouseId, current.enterpriseId());
        return toResponse(new WarehouseRecord(current.id(), current.enterpriseId(), current.code(),
                current.name(), current.address(), status, current.createdAt(), updatedAt));
    }

    /** DTO là ranh giới API; mapper này ngăn entity/JDBC record rò ra controller. */
    private WarehouseResponse toResponse(WarehouseRecord warehouse) {
        return new WarehouseResponse(warehouse.id(), warehouse.enterpriseId(), warehouse.code(),
                warehouse.name(), warehouse.address(), warehouse.status(),
                warehouse.createdAt(), warehouse.updatedAt());
    }

    /** Mã kho không phân biệt chữ hoa/thường để tránh tạo hai mã nhìn giống nhau. */
    private String normalizeCode(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }

    /** Chuỗi tùy chọn rỗng được lưu thành SQL NULL thay vì lưu khoảng trắng. */
    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /** Dùng cùng loại lỗi NOT_FOUND cho cả truy vấn và cập nhật một kho không tồn tại. */
    private ResourceNotFoundException notFound(String warehouseId) {
        return new ResourceNotFoundException("Không tìm thấy Warehouse: " + warehouseId);
    }

    /** Ghi lệnh xóa cache ID và danh sách enterprise cùng transaction với thay đổi Warehouse. */
    private void scheduleWarehouseInvalidation(String warehouseId, String enterpriseId) {
        invalidationOutboxRepository.save(new CacheInvalidationOutboxEvent(
                "inventory-warehouse-by-id-v1", "id:" + warehouseId));
        invalidationOutboxRepository.save(new CacheInvalidationOutboxEvent(
                "inventory-warehouse-list-by-enterprise-v1", "enterprise:" + enterpriseId));
    }
}
