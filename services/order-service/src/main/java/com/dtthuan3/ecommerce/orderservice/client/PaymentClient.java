package com.dtthuan3.ecommerce.orderservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import java.util.Map;

/**
 * Goi payment-service (contract khoa cung docs/PAYMENT_SHIPPING_PLAN.md §3.6).
 * Idempotency-Key bat buoc tren POST /payments — retry khong tao payment thu 2.
 */
@FeignClient(name = "payment-service")
public interface PaymentClient {

    /** Tao payment: body {orderId, method, amount, currency, description?}. */
    @PostMapping("/payments")
    Map<String, Object> create(@RequestHeader("Idempotency-Key") String idempotencyKey,
                               @RequestBody Map<String, Object> request);

    /** Tra trang thai payment khi mat response (UNKNOWN -> query, khong retry charge mù). */
    @GetMapping("/payments/{id}")
    Map<String, Object> get(@PathVariable("id") String paymentId);

    /** Order tra cuu payment theo orderId khi mat response. */
    @GetMapping("/payments/by-order/{orderId}")
    Map<String, Object> getByOrder(@PathVariable("orderId") String orderId);

    /** Huy payment PENDING (khi don bi huy). */
    @PostMapping("/payments/{id}/cancel")
    Map<String, Object> cancel(@PathVariable("id") String paymentId);
}
