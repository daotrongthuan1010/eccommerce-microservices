package com.dtthuan3.ecommerce.inventoryservice.integration;

import com.dtthuan3.ecommerce.inventoryservice.dto.request.ReleaseInventoryReservationRequest;
import com.dtthuan3.ecommerce.inventoryservice.repository.InventoryReservationRepository;
import com.dtthuan3.ecommerce.inventoryservice.repository.InventoryReservationRepository.ReservationHeaderRecord;
import com.dtthuan3.ecommerce.inventoryservice.service.InventoryReservationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * §ADR-0001 issue #4: nửa compensation phía inventory của checkout saga.
 * Order phat OrderPaymentFailed khi payment fail/expire -> listener nay
 * release reservation HELD cua don ngay thay vi cho TTL 15 phut.
 *
 * <p>Idempotent: release co operationId duy nhat theo orderId (release-order-{id});
 * event trung hoac don khong con reservation HELD thi bo qua an toan.
 * Event khong hop -> rethrow de DefaultErrorHandler retry/DLT (khong nuot).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderPaymentFailedListener {

    private final ObjectMapper objectMapper;
    private final InventoryReservationService reservationService;
    private final InventoryReservationRepository reservationRepository;

    @Value("${inventory.kafka.order-events-topic:order-events}")
    private String orderEventsTopic;

    @KafkaListener(topics = "${inventory.kafka.order-events-topic:order-events}")
    public void onOrderEvent(String message) {
        try {
            JsonNode event = objectMapper.readTree(message);
            String eventType = text(event, "eventType");
            String orderId = text(event, "orderId");

            // Chi quan tam event compensation; cac event Order khac bo qua
            if (!"OrderPaymentFailed".equals(eventType)) {
                log.debug("Bo qua order event khong lien quan reservation: {}", eventType);
                return;
            }
            if (orderId == null || orderId.isBlank()) {
                log.warn("OrderPaymentFailed thieu orderId — bo qua");
                return;
            }

            Optional<ReservationHeaderRecord> held = reservationRepository.findHeldByOrderId(orderId);
            if (held.isEmpty()) {
                // Don chua reserve hoac da release/commit — event den lai an toan bo qua
                log.info("Order {} khong con reservation HELD — khong can release", orderId);
                return;
            }

            ReservationHeaderRecord header = held.get();
            ReleaseInventoryReservationRequest request = new ReleaseInventoryReservationRequest(
                    "release-order-" + orderId,
                    "Compensation: payment " + text(event, "paymentEventType"),
                    header.version(),
                    List.of()); // release toan bo residual
            reservationService.release(header.id(), request, "saga:order-payment-failed");
            log.info("Compensation: released reservation {} cho order {} (payment {})",
                    header.id(), orderId, text(event, "paymentEventType"));
        } catch (Exception e) {
            // §ADR-0002: rethrow — retry/DLT, khong nuot lam event mat
            log.error("Xu ly OrderPaymentFailed that bai, rethrow: {}", e.getMessage(), e);
            throw new RuntimeException("Xu ly OrderPaymentFailed that bai", e);
        }
    }

    private static String text(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return v == null || v.isNull() ? null : v.asText();
    }
}
