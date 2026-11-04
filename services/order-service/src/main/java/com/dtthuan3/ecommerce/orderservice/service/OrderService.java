package com.dtthuan3.ecommerce.orderservice.service;

import com.dtthuan3.ecommerce.orderservice.dto.request.CheckoutRequest;
import com.dtthuan3.ecommerce.orderservice.dto.response.CheckoutResponse;
import com.dtthuan3.ecommerce.orderservice.dto.response.OrderResponse;
import org.springframework.data.domain.Page;

/** Order la orchestrator duy nhat (D1): giu ton -> tao payment -> nhan event ket qua. */
public interface OrderService {

    /**
     * Dat hang tu gio (ORD-UC01). Idempotent theo idempotencyKey (ORD-UC03):
     * cung key + cung request -> tra lai don da tao, khong tao don thu 2.
     * Cung key + khac request -> CONFLICT.
     */
    CheckoutResponse checkout(String userId, String idempotencyKey, CheckoutRequest request);

    Page<OrderResponse> myOrders(String userId, int page, int size);

    OrderResponse myOrder(String userId, String orderId);

    OrderResponse cancel(String userId, String orderId, String reason);
}
