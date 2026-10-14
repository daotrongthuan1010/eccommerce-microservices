package com.dtthuan3.ecommerce.inventoryservice.service;

import com.dtthuan3.ecommerce.common.exception.BusinessException;
import com.dtthuan3.ecommerce.common.exception.ErrorCode;
import com.dtthuan3.ecommerce.common.exception.ResourceNotFoundException;
import com.dtthuan3.ecommerce.inventoryservice.entity.Inventory;
import com.dtthuan3.ecommerce.inventoryservice.repository.InventoryLocationAccessRepository;
import com.dtthuan3.ecommerce.inventoryservice.repository.InventoryLocationAccessRepository.LocationSnapshot;
import com.dtthuan3.ecommerce.inventoryservice.repository.InventoryLocationAccessRepository.WarehouseSnapshot;
import com.dtthuan3.ecommerce.inventoryservice.port.ActiveWarehouseValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Kiểm tra trạng thái Warehouse và Location trước khi thay đổi dữ liệu tồn.
 * Service chỉ phụ thuộc repository interface; truy vấn chi tiết và khóa dòng do adapter JDBC đảm nhiệm.
 */
@Service
@RequiredArgsConstructor
public class InventoryLocationAccessService implements ActiveWarehouseValidator {
    /** Repository port dùng đọc projection và Location theo lô, không phụ thuộc Spring Data JPA. */
    private final InventoryLocationAccessRepository accessRepository;

    /** Cho phép tương thích các nghiệp vụ cũ khi Warehouse Service chưa phát projection. */
    @Value("${inventory.warehouse-projection.required:false}")
    private boolean warehouseProjectionRequired;

    /** Dựng một vị trí từ Inventory và dùng chung quy tắc kiểm tra theo lô. */
    @Transactional
    public void requireWritablePosition(String enterpriseId, String warehouseId, String locationId) {
        requireWritablePositions(List.of(new Position(enterpriseId, warehouseId, locationId)));
    }

    /**
     * Kiểm tra nhiều vị trí trong hai truy vấn theo lô thay vì N+1.
     * Location và Warehouse được sắp xếp theo ID trước khi JDBC lấy khóa SHARE để các transaction
     * cạnh tranh cùng một tập đối tượng khóa theo thứ tự nhất quán và giữ khóa đến khi transaction kết thúc.
     */
    @Transactional
    public void requireWritablePositions(Collection<Position> requestedPositions) {
        if (requestedPositions == null || requestedPositions.isEmpty()) return;

        List<Position> positions = requestedPositions.stream()
                .distinct()
                .sorted(Comparator.comparing(Position::locationId).thenComparing(Position::warehouseId))
                .toList();
        List<String> locationIds = positions.stream()
                .map(Position::locationId)
                .distinct()
                .sorted()
                .toList();
        List<String> warehouseIds = positions.stream()
                .map(Position::warehouseId)
                .distinct()
                .sorted()
                .toList();

        // Mỗi repository method phát một SELECT theo lô; chỉ lấy cột cần để xác thực.
        Map<String, LocationSnapshot> locations = accessRepository.findLocationsForShare(locationIds).stream()
                .collect(Collectors.toMap(LocationSnapshot::id, Function.identity()));
        Map<String, WarehouseSnapshot> warehouses = accessRepository.findWarehousesForShare(warehouseIds).stream()
                .collect(Collectors.toMap(WarehouseSnapshot::warehouseId, Function.identity()));

        for (Position position : positions) {
            LocationSnapshot location = locations.get(position.locationId());
            // Không tiết lộ Location khác kho: ID không tồn tại và ID thuộc kho khác cùng trả về not found.
            if (location == null || !location.warehouseId().equals(position.warehouseId())) {
                throw new ResourceNotFoundException(
                        "Không tìm thấy Location trong Warehouse đã chỉ định: " + position.locationId());
            }

            requireActiveWarehouse(position.warehouseId(), position.enterpriseId(), warehouses);
            if (!location.active()) {
                throw new BusinessException(ErrorCode.FORBIDDEN,
                        "Không thể ghi nhận thay đổi tồn kho tại Location đã ngừng hoạt động");
            }
        }
    }

    /** Chuyển Inventory thành khóa vị trí để kiểm tra quyền ghi trước thao tác tồn. */
    @Transactional
    public void requireWritableInventory(Inventory inventory) {
        requireWritablePosition(inventory.getEnterpriseId(), inventory.getWarehouseId(), inventory.getLocationId());
    }

    /**
     * Kiểm tra kho theo chế độ tương thích: nếu cấu hình projection bắt buộc thì kho chưa có dữ liệu
     * sẽ bị từ chối; nếu không, các luồng cũ được phép tiếp tục khi Warehouse Service chưa tích hợp.
     */
    @Transactional(readOnly = true)
    public void requireActiveWarehouse(String warehouseId, String enterpriseId) {
        WarehouseSnapshot warehouse = accessRepository.findWarehouseForShare(warehouseId).orElse(null);
        requireActiveWarehouse(warehouseId, enterpriseId, warehouse == null
                ? Map.of()
                : Map.of(warehouseId, warehouse));
    }

    /**
     * Kiểm tra nghiêm ngặt cho workflow không được tạo dữ liệu trên một kho chưa biết.
     * Method này luôn từ chối nếu chưa nhận được projection, bất kể chế độ tương thích của API tồn cũ.
     */
    @Override
    @Transactional(readOnly = true)
    public void requireKnownActiveWarehouse(String warehouseId) {
        WarehouseSnapshot warehouse = accessRepository.findWarehouseForShare(warehouseId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Chưa nhận được projection của Warehouse: " + warehouseId));
        requireActiveWarehouse(warehouseId, null, Map.of(warehouseId, warehouse));
    }

    /** Áp dụng chung quy tắc tồn tại, trạng thái ACTIVE và enterprise ownership cho từng vị trí. */
    private void requireActiveWarehouse(String warehouseId, String enterpriseId,
                                        Map<String, WarehouseSnapshot> warehouses) {
        WarehouseSnapshot warehouse = warehouses.get(warehouseId);
        if (warehouse == null) {
            if (warehouseProjectionRequired) {
                throw new ResourceNotFoundException("Chưa nhận được projection của Warehouse: " + warehouseId);
            }
            return;
        }
        if (!"ACTIVE".equals(warehouse.status())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Warehouse không hoạt động: " + warehouseId);
        }
        if (enterpriseId != null && !enterpriseId.equals(warehouse.enterpriseId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN,
                    "Warehouse không thuộc enterpriseId đã cung cấp: " + warehouseId);
        }
    }

    /** Bộ khóa tối thiểu để xác định một vị trí cần kiểm tra. */
    public record Position(String enterpriseId, String warehouseId, String locationId) {}
}
