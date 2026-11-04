package com.dtthuan3.ecommerce.inventoryservice.port;

/**
 * Cổng nghiệp vụ xác nhận kho tồn tại và đang được phép tiếp nhận hàng.
 * Interface này định nghĩa cổng tích hợp mà use case Inventory phụ thuộc. Adapter cụ thể có thể thay đổi mà
 * service không phải phụ thuộc trực tiếp thư viện hoặc giao thức bên ngoài.
 */
public interface ActiveWarehouseValidator {
    /** Từ chối nếu chưa có projection của kho hoặc trạng thái hiện tại không phải ACTIVE. */
    void requireKnownActiveWarehouse(String warehouseId);
}
