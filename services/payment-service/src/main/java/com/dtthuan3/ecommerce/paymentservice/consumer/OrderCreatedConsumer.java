package com.dtthuan3.ecommerce.paymentservice.consumer;

import com.dtthuan3.ecommerce.paymentservice.dto.request.CreatePaymentRequest;
import com.dtthuan3.ecommerce.paymentservice.entity.Payment;
import com.dtthuan3.ecommerce.paymentservice.repository.PaymentRepository;
import com.dtthuan3.ecommerce.paymentservice.service.PaymentService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Nhan OrderCreated tu topic order-events (outbox cua order-service) va tao
 * payment PENDING cho don online (VNPAY/ZALOPAY) — buoc bi thieu cua luong
 * checkout: order-service chi tao order + event, payment phai duoc tao o day
 * theo kien truc event-driven (khong couple order -> payment sync).
 *
 * <p>Idempotent: da co payment cho orderId -> bo qua (at-least-once Kafka).
 * COD khong tao payment (thu tien khi giao hang). Event sai shape -> warn + bo
 * qua (khong retry vo ich); loi tao payment that bai -> nem de Kafka retry,
 * sau het retry se vao DLT theo cau hinh container factory.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderCreatedConsumer {

    private final PaymentService paymentService;
    private final PaymentRepository payments;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "${payment.kafka.order-events-topic:order-events}",
            groupId = "${payment.kafka.order-events-group:payment-order-events}")
    public void onOrderCreated(String message) {
        JsonNode event;
        try {
            event = objectMapper.readTree(message);
        } catch (Exception e) {
            log.warn("Bo qua order event sai JSON: {}", truncate(message));
            return;
        }
        String eventType = text(event, "eventType");
        String orderId = text(event, "orderId");
        String paymentMethod = text(event, "paymentMethod");

        if (!"OrderCreated".equals(eventType) || orderId == null) {
            log.debug("Event {} khong phai OrderCreated, bo qua", eventType);
            return;
        }
        // COD: khong co payment online — thu tien khi giao hang.
        if (paymentMethod == null || "COD".equalsIgnoreCase(paymentMethod)) {
            log.debug("Order {} la COD, khong tao payment online", orderId);
            return;
        }
        // Idempotent: Kafka at-least-once co the phat lai event.
        Optional<Payment> existing = payments.findFirstByOrderIdOrderByCreatedAtDesc(orderId);
        if (existing.isPresent()) {
            log.debug("Payment cho order {} da ton tai ({}), bo qua",
                    orderId, existing.get().getStatus());
            return;
        }

        long amount = event.path("totalAmount").asLong(0);
        if (amount <= 0) {
            log.warn("Order {} totalAmount={} khong hop le, bo qua tao payment", orderId, amount);
            return;
        }

        CreatePaymentRequest request = new CreatePaymentRequest(
                orderId,
                com.dtthuan3.ecommerce.paymentservice.domain.PaymentMethod.valueOf(paymentMethod.toUpperCase()),
                amount,
                text(event, "currency"),
                null,
                "Thanh toan don " + text(event, "orderCode"));

        // Idempotency-Key = orderId: retry event khong tao payment thu 2
        // (PaymentService cung kiem tra hash + idempotency key).
        paymentService.create(request, "order-" + orderId, "system-order-consumer");
        log.info("Created {} payment for order {} amount {}",
                paymentMethod, orderId, amount);
    }

    private static String text(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return v == null || v.isNull() ? null : v.asText();
    }

    private static String truncate(String s) {
        return s == null ? "null" : (s.length() > 200 ? s.substring(0, 200) + "…" : s);
    }
}
