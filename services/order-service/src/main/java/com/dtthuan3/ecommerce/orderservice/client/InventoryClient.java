package com.dtthuan3.ecommerce.orderservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Goi inventory-service (qua Eureka lb://inventory-service).
 * Order la orchestrator: giu hang dong bo truoc khi tao payment
 * (PAYMENT_SHIPPING_PLAN D1: payment/shipping khong goi nguoc order).
 * JWT nguoi dung duoc forward boi FeignSecurityConfig.
 */
@FeignClient(name = "inventory-service")
public interface InventoryClient {

    /** Tao reservation: all-or-nothing, idempotent theo (reservationId, operationId). */
    @PostMapping("/inventory/reservations")
    Map<String, Object> reserve(@RequestBody Map<String, Object> request);

    /** Tra trang thai reservation sau timeout (UNKNOWN -> query, khong retry mù). */
    @GetMapping("/inventory/reservations/{reservationId}")
    Map<String, Object> getReservation(@PathVariable("reservationId") String reservationId);

    /** Nha reservation khi don huy. */
    @PostMapping("/inventory/reservations/{reservationId}/release")
    Map<String, Object> release(@PathVariable("reservationId") String reservationId,
                                @RequestBody Map<String, Object> request);

    /** Commit sau khi payment duoc xac nhan. */
    @PostMapping("/inventory/reservations/{reservationId}/commit")
    Map<String, Object> commit(@PathVariable("reservationId") String reservationId,
                                @RequestBody Map<String, Object> request);

    /** Body reserve theo CreateInventoryReservationRequest cua inventory-service. */
    record ReserveLine(String inventoryId, java.math.BigDecimal quantity) {}

    record ReserveRequest(String reservationId, String orderId, String operationId,
                          Instant expiresAt, List<ReserveLine> lines) {}
}
