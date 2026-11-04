package com.dtthuan3.ecommerce.inventoryservice.service;

import com.dtthuan3.ecommerce.inventoryservice.dto.request.ReplayProductEventDltRequest;
import com.dtthuan3.ecommerce.inventoryservice.dto.response.ReplayProductEventDltResponse;

/**
 * Cổng nghiệp vụ replay có kiểm soát một Product event từ dead-letter topic.
 * Interface này công bố các use case Inventory cho controller, worker và adapter gọi tới. Class triển khai
 * nằm ở service/impl, vì vậy bên gọi phụ thuộc vào hợp đồng thay vì chi tiết xử lý.
 */
public interface InventoryDltReplayService {
    ReplayProductEventDltResponse replayProductEvent(ReplayProductEventDltRequest request, String operator);
}
