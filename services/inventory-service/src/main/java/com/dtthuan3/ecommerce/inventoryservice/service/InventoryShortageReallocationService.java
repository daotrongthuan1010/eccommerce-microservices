package com.dtthuan3.ecommerce.inventoryservice.service;

import com.dtthuan3.ecommerce.inventoryservice.dto.request.ReserveShortageReallocationRequest;
import com.dtthuan3.ecommerce.inventoryservice.dto.response.ShortageReallocationReservationResponse;

/** Use case giữ lượng thay thế cho shortage case trước khi giải phóng allocation cũ. */
public interface InventoryShortageReallocationService {
    ShortageReallocationReservationResponse reserve(String shortageCaseId,
                                                   ReserveShortageReallocationRequest request,
                                                   String reservedBy);
}
