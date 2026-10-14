package com.dtthuan3.ecommerce.inventoryservice.mapper;

import com.dtthuan3.ecommerce.inventoryservice.dto.response.LocationResponse;
import com.dtthuan3.ecommerce.inventoryservice.entity.InventoryLocation;
import org.springframework.stereotype.Component;

/** Tập trung chuyển đổi entity sang response để controller/service không tự dựng DTO thủ công. */
@Component
public class InventoryLocationMapper {

    /** Chỉ sao chép dữ liệu API cần dùng; không để JPA entity hoặc quan hệ persistence lọt ra ngoài. */
    public LocationResponse toResponse(InventoryLocation location) {
        return new LocationResponse(location.getId(), location.getWarehouseId(), location.getCode(),
                location.getName(), location.isActive());
    }
}
