package com.dtthuan3.ecommerce.notificationservice.consumer;

import com.dtthuan3.ecommerce.notificationservice.entity.NotificationInbox;
import com.dtthuan3.ecommerce.notificationservice.entity.NotificationRequest;
import com.dtthuan3.ecommerce.notificationservice.repository.NotificationInboxRepository;
import com.dtthuan3.ecommerce.notificationservice.repository.NotificationRequestRepository;
import com.dtthuan3.ecommerce.notificationservice.service.TemplateRenderer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Nhan OrderConfirmed/OrderCanceled tu topic order-events (contract 03-kafka-events.md:
 * order -> notification). Inbox dedup theo (eventId, source); businessKey
 * businessEventId|recipientId|channel|templateKey chong tao request trung (NOTI-03).
 * Khong gui mail trong listener — chi tao request QUEUED; worker nen gui sau (NOTI-01).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {

    private static final String SOURCE = "order-service";
    private static final String CHANNEL_EMAIL = "EMAIL";

    private final NotificationInboxRepository inbox;
    private final NotificationRequestRepository requests;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "${notification.kafka.order-events-topic:order-events}")
    @Transactional
    public void onOrderEvent(String message) {
        try {
            JsonNode event = objectMapper.readTree(message);
            String eventId = text(event, "eventId");
            String eventType = text(event, "eventType");
            String orderId = text(event, "orderId");
            String userId = text(event, "userId");

            if (eventId == null || eventType == null || orderId == null || userId == null) {
                log.warn("Bo qua order event sai shape: {}", truncate(message));
                return;
            }

            // Inbox dedup: da xu ly roi thi bo qua (at-least-once -> effectively-once)
            if (inbox.existsByEventIdAndSource(eventId, SOURCE)) {
                log.debug("Order event {} da xu ly, bo qua", eventId);
                return;
            }

            switch (eventType) {
                case "OrderConfirmed", "OrderCanceled" -> createRequest(eventId, eventType, event);
                default -> log.debug("Order event {} khong tao thong bao: bo qua", eventType);
            }

            NotificationInbox processed = new NotificationInbox();
            processed.setEventId(eventId);
            processed.setSource(SOURCE);
            processed.setEventType(eventType);
            processed.setProcessedAt(Instant.now());
            inbox.save(processed);

        } catch (Exception e) {
            // Poison message: log + khong rethrow de khong block consumer; DLQ sau nay
            log.error("Xu ly order event that bai: {} — {}", e.getMessage(), truncate(message), e);
        }
    }

    /**
     * Tao NotificationRequest QUEUED cung transaction voi inbox ghi o listener.
     * businessKey = businessEventId|recipientId|channel|templateKey (NOTI-03).
     */
    private void createRequest(String eventId, String eventType, JsonNode event) {
        String userId = text(event, "userId");
        String orderId = text(event, "orderId");
        String orderCode = text(event, "orderCode");
        String templateKey = "OrderConfirmed".equals(eventType)
                ? "order-confirmed.vi"
                : "order-canceled.vi";

        String businessKey = eventId + "|" + userId + "|" + CHANNEL_EMAIL + "|" + templateKey;
        if (requests.existsByBusinessKey(businessKey)) {
            log.info("Request {} da ton tai (businessKey trung) — bo qua", businessKey);
            return;
        }

        Map<String, Object> vars = new HashMap<>();
        vars.put("orderId", orderId);
        vars.put("orderCode", orderCode == null ? orderId : orderCode);
        vars.put("userId", userId);
        long total = event.path("totalAmount").asLong(0);
        vars.put("totalAmount", String.format("%,d VND", total));

        String subject = "OrderConfirmed".equals(eventType)
                ? "Đơn hàng " + (orderCode == null ? orderId : orderCode) + " đã được xác nhận"
                : "Đơn hàng " + (orderCode == null ? orderId : orderCode) + " đã bị hủy";
        String body = "OrderConfirmed".equals(eventType)
                ? TemplateRenderer.render("""
                        Xin chào {{userId}},

                        Đơn hàng {{orderCode}} của bạn đã được xác nhận.
                        Tổng tiền: {{totalAmount}}.

                        Cảm ơn bạn đã mua sắm tại shop.
                        """, vars)
                : TemplateRenderer.render("""
                        Xin chào {{userId}},

                        Đơn hàng {{orderCode}} của bạn đã bị hủy.
                        Nếu có câu hỏi, vui lòng liên hệ bộ phận hỗ trợ.

                        Cảm ơn bạn.
                        """, vars);

        NotificationRequest request = new NotificationRequest();
        request.setId(UUID.randomUUID().toString());
        request.setBusinessEventId(eventId);
        request.setRecipientId(userId);
        request.setChannel(CHANNEL_EMAIL);
        request.setTemplateKey(templateKey);
        request.setPurpose("TRANSACTIONAL");
        request.setBusinessKey(businessKey);
        request.setSubject(subject);
        request.setBody(body);
        request.setStatus("QUEUED");
        request.setAttempts(0);
        request.setNextAttemptAt(Instant.now());
        request.setOrderId(orderId);
        request.setCorrelationId(eventId);
        requests.save(request);
        log.info("Tao NotificationRequest {} cho {} orderId={} businessKey={}",
                request.getId(), eventType, orderId, businessKey);
    }

    private static String text(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return v == null || v.isNull() ? null : v.asText();
    }

    private static String truncate(String s) {
        return s == null ? "" : (s.length() > 300 ? s.substring(0, 300) + "..." : s);
    }
}
