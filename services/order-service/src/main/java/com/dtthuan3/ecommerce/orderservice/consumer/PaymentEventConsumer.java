package com.dtthuan3.ecommerce.orderservice.consumer;

import com.dtthuan3.ecommerce.orderservice.entity.OrderInbox;
import com.dtthuan3.ecommerce.orderservice.repository.OrderInboxRepository;
import com.dtthuan3.ecommerce.orderservice.repository.OrderRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Nhan PaymentSucceeded/PaymentFailed/PaymentExpired tu topic payment-events
 * (contract docs/PAYMENT_SHIPPING_PLAN.md §3.6). Inbox dedup theo (eventId, source):
 * event den lai/khong theo thu tu khong lam don nhay trang thai 2 lan.
 * PAY-UC02: timeout nhung provider da thu -> event Succeeded den muon van duoc chap nhan
 * (latePayment=true) — don PENDING_PAYMENT chuyen PAID, khong tu coi la FAIL.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventConsumer {

    private static final String SOURCE = "payment-service";

    private final OrderInboxRepository inbox;
    private final OrderRepository orderRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "${order.kafka.payment-events-topic:payment-events}")
    @Transactional
    public void onPaymentEvent(String message) {
        try {
            JsonNode event = objectMapper.readTree(message);

            String eventId = text(event, "eventId");
            String eventType = text(event, "eventType");
            String orderId = text(event, "orderId");

            if (eventId == null || eventType == null || orderId == null) {
                log.warn("Bo qua payment event sai shape: {}", truncate(message));
                return;
            }

            // Inbox dedup: da xu ly roi thi bo qua (at-least-once -> effectively-once)
            if (inbox.existsByEventIdAndSource(eventId, SOURCE)) {
                log.debug("Payment event {} da xu ly, bo qua", eventId);
                return;
            }

            switch (eventType) {
                case "PaymentSucceeded" -> markPaid(orderId, event);
                case "PaymentFailed", "PaymentExpired" -> markFailed(orderId, eventType);
                default -> log.debug("Payment event {} khong lien quan don: bo qua", eventType);
            }

            OrderInbox processed = new OrderInbox();
            processed.setEventId(eventId);
            processed.setSource(SOURCE);
            processed.setEventType(eventType);
            processed.setProcessedAt(Instant.now());
            inbox.save(processed);

        } catch (Exception e) {
            // Poison message: log + khong rethrow de khong block consumer; can DLT sau nay
            log.error("Xu ly payment event that bai: {} — {}", e.getMessage(), truncate(message), e);
        }
    }

    private void markPaid(String orderId, JsonNode event) {
        orderRepository.findById(orderId).ifPresent(order -> {
            if (!"PENDING_PAYMENT".equals(order.getStatus())) {
                log.info("Order {} dang {} — bo qua PaymentSucceeded (idempotent)", orderId, order.getStatus());
                return;
            }
            order.setStatus("PAID");
            order.setPaymentId(text(event, "paymentId"));
            orderRepository.save(order);
            log.info("Order {} -> PAID (payment {})", orderId, text(event, "paymentId"));
        });
    }

    private void markFailed(String orderId, String eventType) {
        orderRepository.findById(orderId).ifPresent(order -> {
            if (!"PENDING_PAYMENT".equals(order.getStatus())) {
                log.info("Order {} dang {} — bo qua {} (idempotent)", orderId, order.getStatus(), eventType);
                return;
            }
            order.setStatus("PAYMENT_FAILED");
            orderRepository.save(order);
            log.info("Order {} -> PAYMENT_FAILED ({})", orderId, eventType);
        });
    }

    private static String text(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return v == null || v.isNull() ? null : v.asText();
    }

    private static String truncate(String s) {
        return s != null && s.length() > 300 ? s.substring(0, 300) + "…" : s;
    }
}
