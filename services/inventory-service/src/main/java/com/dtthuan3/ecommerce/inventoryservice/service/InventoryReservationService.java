package com.dtthuan3.ecommerce.inventoryservice.service;

import com.dtthuan3.ecommerce.inventoryservice.dto.request.CommitInventoryReservationRequest;
import com.dtthuan3.ecommerce.inventoryservice.dto.request.CreateInventoryReservationRequest;
import com.dtthuan3.ecommerce.inventoryservice.dto.request.DispatchInventoryReservationRequest;
import com.dtthuan3.ecommerce.inventoryservice.dto.request.ReleaseInventoryReservationRequest;
import com.dtthuan3.ecommerce.inventoryservice.dto.response.InventoryReservationResponse;

/**
 * Use case quản lý giữ hàng theo reservation/order và tiến độ từng dòng hàng.
 * Interface này công bố các use case Inventory cho controller, worker và adapter gọi tới. Class triển khai
 * nằm ở service/impl, vì vậy bên gọi phụ thuộc vào hợp đồng thay vì chi tiết xử lý.
 */
public interface InventoryReservationService {
    InventoryReservationResponse getById(String reservationId);
    InventoryReservationResponse expire(String reservationId, String expiredBy);
    InventoryReservationResponse reserve(CreateInventoryReservationRequest request, String createdBy);
    InventoryReservationResponse commit(String reservationId, CommitInventoryReservationRequest request,
                                        String committedBy);
    InventoryReservationResponse release(String reservationId, ReleaseInventoryReservationRequest request,
                                         String releasedBy);
    InventoryReservationResponse dispatch(String reservationId, DispatchInventoryReservationRequest request,
                                          String dispatchedBy);
}
