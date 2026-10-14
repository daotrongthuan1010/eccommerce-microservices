package com.dtthuan3.ecommerce.inventoryservice.port;

/** Cổng nghiệp vụ xác nhận kho tồn tại và đang được phép tiếp nhận hàng. */
public interface ActiveWarehouseValidator {
    /** Từ chối nếu chưa có projection của kho hoặc trạng thái hiện tại không phải ACTIVE. */
    void requireKnownActiveWarehouse(String warehouseId);
}
