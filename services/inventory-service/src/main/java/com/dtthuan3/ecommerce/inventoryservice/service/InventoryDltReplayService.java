package com.dtthuan3.ecommerce.inventoryservice.service;

import com.dtthuan3.ecommerce.inventoryservice.dto.request.ReplayProductEventDltRequest;
import com.dtthuan3.ecommerce.inventoryservice.dto.response.ReplayProductEventDltResponse;

/** Cổng nghiệp vụ replay có kiểm soát một Product event từ dead-letter topic. */
public interface InventoryDltReplayService {
    ReplayProductEventDltResponse replayProductEvent(ReplayProductEventDltRequest request, String operator);
}
