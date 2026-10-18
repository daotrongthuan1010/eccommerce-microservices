package com.dtthuan3.ecommerce.inventoryservice.port;

import java.util.Collection;

/** Cổng nghiệp vụ xác thực SKU, giúp service không phụ thuộc Product Service hoặc HTTP client cụ thể. */
public interface ActiveSkuValidator {
    /** Từ chối nếu có SKU không hoạt động hoặc không thể xác nhận an toàn danh sách đầu vào. */
    void requireActiveSkus(Collection<String> skuCodes);
}
