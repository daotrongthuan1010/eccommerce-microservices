package com.dtthuan3.ecommerce.orderservice.consumer;

import com.dtthuan3.ecommerce.orderservice.domain.OrderStatus;
import com.dtthuan3.ecommerce.orderservice.entity.Order;
import com.dtthuan3.ecommerce.orderservice.entity.OrderInbox;
import com.dtthuan3.ecommerce.orderservice.repository.OrderInboxRepository;
import com.dtthuan3.ecommerce.orderservice.repository.OrderRepository;
import com.dtthuan3.ecommerce.orderservice.service.OrderService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Nhan PaymentSucceeded/PaymentFailed/PaymentExpired tu topic payment-events
 * (contract docs/PAYMENT_SHIPPING_PLAN.md §3.6). Inbox dedup theo (eventId, source):
 * event den lai/khong theo thu tu khong lam don nhay trang thai 2 lan.
 * PAY-UC02: timeout nhung provider da thu -> event Succeeded den muon van duoc chap nhan
 * (latePayment=true) — don PENDING_PAYMENT chuyen PAID, khong tu coi la FAIL.
 *
 * <p>2026-11-05 e2e-fix: khi don chuyen PAID, PHAT TIEP event OrderConfirmed
 * (kafka send trong cung transaction — don da co outbox pattern cho checkout,
 * day dung vi inbox dedup ben notification theo eventId moi) de
 * notification-service gui mail xac nhan. Truoc day khong ai phat event nay —
 * notification lang nghe OrderConfirmed nhung khong bao gio nhan duoc.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventConsumer {

    private static final String SOURCE = "payment-service";

    private final OrderInboxRepository inbox;
    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, String> kafka;

    @Value("${order.kafka.order-events-topic:order-events}")
    private String orderEventsTopic;

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
                case "PaymentFailed", "PaymentExpired" -> markFailed(orderId, event);
                default -> log.debug("Payment event {} khong lien quan don: bo qua", eventType);
            }

            OrderInbox processed = new OrderInbox();
            processed.setEventId(eventId);
            processed.setSource(SOURCE);
            processed.setEventType(eventType);
            processed.setProcessedAt(Instant.now());
            inbox.save(processed);

        } catch (Exception e) {
            // §issue #5: poison message phai rethrow — DefaultErrorHandler retry
            // + DLT (cau hinh trong KafkaConfig) thay vi nuot lam event mat.
            // Inbox chua ghi nen redeliver xu ly lai; het retry -> DLT cho replay.
            log.error("Xu ly payment event that bai, rethrow de retry/DLT: {} — {}", e.getMessage(), truncate(message), e);
            throw new RuntimeException("Xu ly payment event that bai", e);
        }
    }

    private void markPaid(String orderId, JsonNode event) {
        orderRepository.findById(orderId).ifPresent(order -> {
            // §review: status legacy ngoai enum -> skip idempotent (inbox van ghi),
            // KHONG throw vi se ban event vao DLT vinh vien, don keo mai
            OrderStatus current = parseStatus(order);
            if (current == null) {
                log.warn("Order {} co status legacy '{}' — skip PaymentSucceeded, can sua du lieu", orderId, order.getStatus());
                return;
            }
            if (current != OrderStatus.PENDING_PAYMENT) {
                log.info("Order {} dang {} — bo qua PaymentSucceeded (idempotent)", orderId, current);
                return;
            }
            OrderStatus.assertTransition(current, OrderStatus.PAID);
            order.setStatus(OrderStatus.PAID.getValue());
            order.setPaymentId(text(event, "paymentId"));
            orderRepository.save(order);
            // Ghi outbox OrderPaid chung transaction — publisher se day len topic order-events
            orderService.publishOrderPaid(order);
            log.info("Order {} -> PAID (payment {})", orderId, text(event, "paymentId"));
            publishOrderConfirmed(order, event);
        });
    }

    /** Parse status an toan: null khi gia tri DB ngoai enum (legacy) thay vi throw. */
    private OrderStatus parseStatus(Order order) {
        try {
            return OrderStatus.fromValue(order.getStatus());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Phat OrderConfirmed sau khi don PAID — notification-service dung event
     * nay de gui mail (templateKey order-confirmed, can userId + orderCode +
     * totalAmount). Kafka send trong transaction: neu fail thi rollback ca
     * trang thai PAID (at-least-once; notification dedup inbox theo eventId).
     */
    private void publishOrderConfirmed(Order order, JsonNode paymentEvent) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("eventId", UUID.randomUUID().toString());
            payload.put("eventType", "OrderConfirmed");
            payload.put("orderId", order.getId());
            payload.put("orderCode", order.getOrderCode());
            payload.put("userId", order.getUserId());
            payload.put("status", order.getStatus());
            payload.put("totalAmount", order.getTotalAmount());
            payload.put("currency", order.getCurrency());
            payload.put("paymentMethod", order.getPaymentMethod());
            payload.put("occurredAt", Instant.now().toString());
            kafka.send(orderEventsTopic, order.getId(), objectMapper.writeValueAsString(payload));
            log.info("Published OrderConfirmed for order {}", order.getId());
        } catch (Exception e) {
            // §ADR-0002 issue #5: KHONG nuot — rethrow de transaction rollback,
            // Kafka redeliver event payment; inbox chua ghi nen xu ly lai tu dau.
            // Truoc day nuot exception = event OrderConfirmed mat vinh vien
            // (mail xac nhan khong bao gio gui, khong ai biet).
            throw new RuntimeException("Publish OrderConfirmed that bai cho order " + order.getId(), e);
        }
    }

    private void markFailed(String orderId, JsonNode event) {
        String eventType = text(event, "eventType");
        Order order = orderRepository.findById(orderId).orElse(null);
        // §code-review: order miss (race/purge) van phat compensation —
        // reservation co the dang HELD ben inventory; listener ben do
        // idempotent nen phat thua la an toan, thieu moi la hai.
        if (order == null) {
            publishOrderPaymentFailed(orderId, event);
            log.warn("Order {} khong ton tai — van phat compensation (inventory tu bo qua)", orderId);
            return;
        }
        // §review: status legacy ngoai enum -> van phat compensation (an toan
        // ben inventory) roi skip, KHONG throw vao DLT vinh vien
        OrderStatus current = parseStatus(order);
        if (current == null) {
            publishOrderPaymentFailed(order, event);
            log.warn("Order {} co status legacy '{}' — van phat compensation, skip doi trang thai", orderId, order.getStatus());
            return;
        }
        if (current != OrderStatus.PENDING_PAYMENT) {
            log.info("Order {} dang {} — bo qua {} (idempotent)", orderId, current, eventType);
            return;
        }
        OrderStatus.assertTransition(current, OrderStatus.PAYMENT_FAILED);
        order.setStatus(OrderStatus.PAYMENT_FAILED.getValue());
        orderRepository.save(order);
        log.info("Order {} -> PAYMENT_FAILED ({})", orderId, eventType);
        // §ADR-0001 issue #4: compensating action — phat event de inventory
        // release reservation ngay thay vi cho TTL 15 phut. Event qua outbox
        // se hoan thien sau (issue #5); tam thoi send + rethrow khi fail
        // de inbox khong ghi, consumer Kafka retry.
        publishOrderPaymentFailed(order, event);
    }

    /**
     * §ADR-0001: event compensation cho saga — inventory-service lang nghe
     * OrderPaymentFailed de release reservation cua don. Send fail phai rethrow
     * (khong nuot) de Kafka retry; inbox chua ghi nen event se xu ly lai.
     */
    private void publishOrderPaymentFailed(Order order, JsonNode paymentEvent) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("eventId", UUID.randomUUID().toString());
            payload.put("eventType", "OrderPaymentFailed");
            payload.put("orderId", order.getId());
            payload.put("orderCode", order.getOrderCode());
            payload.put("userId", order.getUserId());
            payload.put("status", order.getStatus());
            payload.put("paymentEventType", text(paymentEvent, "eventType"));
            payload.put("occurredAt", Instant.now().toString());
            kafka.send(orderEventsTopic, order.getId(), objectMapper.writeValueAsString(payload));
            log.info("Published OrderPaymentFailed (compensation) for order {}", order.getId());
        } catch (Exception e) {
            // §ADR-0002: khong nuot — rethrow de transaction rollback, Kafka retry
            throw new RuntimeException("Publish OrderPaymentFailed that bai cho order " + order.getId(), e);
        }
    }

    /** Bien the order miss: phat compensation voi orderId thoi (khong co Order entity). */
    private void publishOrderPaymentFailed(String orderId, JsonNode paymentEvent) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("eventId", UUID.randomUUID().toString());
            payload.put("eventType", "OrderPaymentFailed");
            payload.put("orderId", orderId);
            payload.put("paymentEventType", text(paymentEvent, "eventType"));
            payload.put("occurredAt", Instant.now().toString());
            kafka.send(orderEventsTopic, orderId, objectMapper.writeValueAsString(payload));
            log.info("Published OrderPaymentFailed (compensation, order-miss) for order {}", orderId);
        } catch (Exception e) {
            throw new RuntimeException("Publish OrderPaymentFailed that bai cho order " + orderId, e);
        }
    }

    private static String text(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return v == null || v.isNull() ? null : v.asText();
    }

    private static String truncate(String s) {
        return s != null && s.length() > 300 ? s.substring(0, 300) + "…" : s;
    }
}
