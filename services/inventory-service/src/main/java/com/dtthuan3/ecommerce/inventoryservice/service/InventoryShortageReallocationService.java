package com.dtthuan3.ecommerce.inventoryservice.service;

import com.dtthuan3.ecommerce.inventoryservice.dto.request.ReserveShortageReallocationRequest;
import com.dtthuan3.ecommerce.inventoryservice.dto.response.ShortageReallocationReservationResponse;

/**
 * Use case giữ lượng thay thế cho shortage case trước khi giải phóng allocation cũ.
 * Interface này công bố các use case Inventory cho controller, worker và adapter gọi tới. Class triển khai
 * nằm ở service/impl, vì vậy bên gọi phụ thuộc vào hợp đồng thay vì chi tiết xử lý.
 */
public interface InventoryShortageReallocationService {
    ShortageReallocationReservationResponse reserve(String shortageCaseId,
                                                   ReserveShortageReallocationRequest request,
                                                   String reservedBy);
}
