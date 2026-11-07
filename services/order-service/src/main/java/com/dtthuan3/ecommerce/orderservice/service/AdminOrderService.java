package com.dtthuan3.ecommerce.orderservice.service;

import com.dtthuan3.ecommerce.orderservice.dto.response.OrderResponse;
import org.springframework.data.domain.Page;

/** Quan tri don hang (ADMIN): list toan bo, chuyen trang thai fulfill, huy. */
public interface AdminOrderService {

    Page<OrderResponse> list(int page, int size, String status, String search);

    OrderResponse detail(String orderId);

    OrderResponse updateStatus(String orderId, String status);

    OrderResponse cancel(String orderId, String reason);
}
