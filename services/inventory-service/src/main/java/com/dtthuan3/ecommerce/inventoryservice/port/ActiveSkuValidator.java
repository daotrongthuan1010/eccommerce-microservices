package com.dtthuan3.ecommerce.inventoryservice.port;

import java.util.Collection;

/**
 * Cổng nghiệp vụ xác thực SKU, giúp service không phụ thuộc Product Service hoặc HTTP client cụ thể.
 * Interface này định nghĩa cổng tích hợp mà use case Inventory phụ thuộc. Adapter cụ thể có thể thay đổi mà
 * service không phải phụ thuộc trực tiếp thư viện hoặc giao thức bên ngoài.
 */
public interface ActiveSkuValidator {
    /** Từ chối nếu có SKU không hoạt động hoặc không thể xác nhận an toàn danh sách đầu vào. */
    void requireActiveSkus(Collection<String> skuCodes);
}
