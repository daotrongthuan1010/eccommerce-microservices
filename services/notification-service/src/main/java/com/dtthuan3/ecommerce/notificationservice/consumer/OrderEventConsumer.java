package com.dtthuan3.ecommerce.notificationservice.consumer;

import com.dtthuan3.ecommerce.notificationservice.entity.NotificationInbox;
import com.dtthuan3.ecommerce.notificationservice.entity.NotificationRequest;
import com.dtthuan3.ecommerce.notificationservice.entity.NotificationTemplate;
import com.dtthuan3.ecommerce.notificationservice.repository.NotificationDeadLetterRepository;
import com.dtthuan3.ecommerce.notificationservice.repository.NotificationInboxRepository;
import com.dtthuan3.ecommerce.notificationservice.repository.NotificationRequestRepository;
import com.dtthuan3.ecommerce.notificationservice.repository.NotificationTemplateRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Nhan OrderConfirmed/OrderCanceled tu topic order-events (contract 03-kafka-events.md).
 *
 * Viec cua consumer (NOTI-UC01 buoc 1-2) — CHI chot du lieu, khong gui, khong render:
 *  1. Inbox dedup theo (eventId, source): event Kafka den lai khong tao request lan 2.
 *  2. businessKey = businessEventId|recipientId|channel|templateKey chong trung (NOTI-03).
 *  3. Chot templateVersion ACTIVE moi nhat + luu bien can thiet (orderCode, totalAmount).
 * Render + gui la viec worker (buoc 4) — tach biet de mail loi khong anh huong consumer.
 * Template khong ton tai -> DLQ TEMPLATE_MISSING (loi cau hinh, khong retry vo ich).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {

    private static final String SOURCE = "order-service";
    private static final String CHANNEL_EMAIL = "EMAIL";
    private static final String LOCALE_VI = "vi";

    private final NotificationInboxRepository inbox;
    private final NotificationRequestRepository requests;
    private final NotificationTemplateRepository templates;
    private final NotificationDeadLetterRepository deadLetters;
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
            // Poison message: log + khong rethrow de khong block partition (DLQ sau nay)
            log.error("Xu ly order event that bai: {} — {}", e.getMessage(), truncate(message), e);
        }
    }

    /**
     * Tao request QUEUED: chot templateVersion + bien. KHONG render, KHONG gui.
     */
    private void createRequest(String eventId, String eventType, JsonNode event) {
        String userId = text(event, "userId");
        String orderId = text(event, "orderId");
        String orderCode = text(event, "orderCode");
        String templateKey = "OrderConfirmed".equals(eventType)
                ? "order-confirmed"
                : "order-canceled";

        // Chot version ACTIVE moi nhat (NOTI-UC04 buoc 5: request chot templateVersion)
        NotificationTemplate template = templates
                .findFirstByTemplateKeyAndChannelAndLocaleAndStatusOrderByVersionDesc(
                        templateKey, CHANNEL_EMAIL, LOCALE_VI, "ACTIVE")
                .orElse(null);
        if (template == null) {
            // Loi cau hinh he thong: khong co template ACTIVE — vao DLQ, khong tao request rot rac
            log.error("Khong tim thay template ACTIVE {} cho event {}", templateKey, eventId);
            deadLetters.save(deadLetter(null, "TEMPLATE_MISSING",
                    "Template " + templateKey + "/" + CHANNEL_EMAIL + "/" + LOCALE_VI + " khong co ban ACTIVE"));
            return;
        }

        String businessKey = eventId + "|" + userId + "|" + CHANNEL_EMAIL + "|" + templateKey;
        if (requests.existsByBusinessKey(businessKey)) {
            log.info("Request businessKey={} da ton tai — bo qua", businessKey);
            return;
        }

        // Bien can thiet cho template, chot tai thoi diem nhan event.
        // Locale cu the: %,d phu thuoc locale JVM (Windows ra "250.000") — ep VI cho dong quan.
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("orderId", orderId);
        variables.put("orderCode", orderCode == null ? orderId : orderCode);
        variables.put("totalAmount", java.text.NumberFormat
                .getNumberInstance(java.util.Locale.of("vi", "VN"))
                .format(event.path("totalAmount").asLong(0)) + " VND");

        NotificationRequest request = new NotificationRequest();
        request.setId(UUID.randomUUID().toString());
        request.setBusinessEventId(eventId);
        request.setRecipientId(userId);
        request.setChannel(CHANNEL_EMAIL);
        request.setTemplateKey(templateKey);
        request.setTemplateVersion(template.getVersion());
        request.setLocale(LOCALE_VI);
        request.setPurpose(template.getPurpose());
        request.setBusinessKey(businessKey);
        request.setVariables(toJson(variables));
        request.setStatus("QUEUED");
        request.setAttempts(0);
        request.setNextAttemptAt(Instant.now());
        request.setOrderId(orderId);
        request.setCorrelationId(eventId);
        requests.save(request);
        log.info("Tao NotificationRequest {} cho {} orderId={} template={}v{}",
                request.getId(), eventType, orderId, templateKey, template.getVersion());
    }

    private String toJson(Map<String, Object> variables) {
        try {
            return objectMapper.writeValueAsString(variables);
        } catch (Exception e) {
            throw new IllegalStateException("Khong serialize duoc bien template", e);
        }
    }

    private com.dtthuan3.ecommerce.notificationservice.entity.NotificationDeadLetter deadLetter(
            String requestId, String reason, String detail) {
        var dlq = new com.dtthuan3.ecommerce.notificationservice.entity.NotificationDeadLetter();
        dlq.setId(UUID.randomUUID().toString());
        dlq.setRequestId(requestId == null ? "n/a" : requestId);
        dlq.setReason(reason);
        dlq.setDetail(detail);
        dlq.setStatus("OPEN");
        dlq.setCreatedAt(Instant.now());
        return dlq;
    }

    private static String text(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return v == null || v.isNull() ? null : v.asText();
    }

    private static String truncate(String s) {
        return s == null ? "" : (s.length() > 300 ? s.substring(0, 300) + "..." : s);
    }
}
