package com.dtthuan3.ecommerce.orderservice.consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dtthuan3.ecommerce.orderservice.entity.Order;
import com.dtthuan3.ecommerce.orderservice.repository.OrderInboxRepository;
import com.dtthuan3.ecommerce.orderservice.repository.OrderRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.kafka.core.KafkaTemplate;

/**
 * SAGA-UC01/UC02 (issue #4, ADR-0001): khi payment FAIL/EXPIRED, saga phải có
 * compensating action — don chuyen PAYMENT_FAILED VA phat event compensation
 * de inventory release reservation. Audit 2026-11-07: hien tai chi set status,
 * hang van bi giu den TTL 15 phut (compensation gap).
 *
 * Test ghi lai HANH VI MONG DOI (spec) — se FAIL cho den khi issue #4 fix.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SagaCompensationTest {

    private static final String FAILED_EVENT = """
            {"eventId":"evt-f1","eventType":"PaymentFailed","paymentId":"pay-f1",
             "orderId":"order-1","method":"VNPAY","amount":200000,"currency":"VND",
             "status":"FAILED","occurredAt":"2026-11-07T10:00:00Z"}
            """;

    private static final String EXPIRED_EVENT = """
            {"eventId":"evt-e1","eventType":"PaymentExpired","paymentId":"pay-e1",
             "orderId":"order-1","method":"COD","amount":200000,"currency":"VND",
             "status":"EXPIRED","occurredAt":"2026-11-07T10:00:00Z"}
            """;

    @Mock private OrderInboxRepository inbox;
    @Mock private OrderRepository orderRepository;
    @Mock private KafkaTemplate<String, String> kafka;

    private PaymentEventConsumer consumer;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() throws Exception {
        consumer = new PaymentEventConsumer(inbox, orderRepository, objectMapper, kafka);
        // @Value field khong tu inject trong unit test — set truc tiep
        var field = PaymentEventConsumer.class.getDeclaredField("orderEventsTopic");
        field.setAccessible(true);
        field.set(consumer, "order-events");
    }

    private Order pendingOrder() {
        Order order = new Order();
        order.setId("order-1");
        order.setUserId("user-1");
        order.setOrderCode("ORD-001");
        order.setStatus("PENDING_PAYMENT");
        return order;
    }

    @Test
    @DisplayName("SAGA-UC01: PaymentFailed -> PAYMENT_FAILED + phat event compensation (OrderPaymentFailed)")
    void paymentFailed_mustPublishCompensationEvent() {
        Order order = pendingOrder();
        when(inbox.existsByEventIdAndSource(anyString(), anyString())).thenReturn(false);
        when(orderRepository.findById("order-1")).thenReturn(Optional.of(order));

        consumer.onPaymentEvent(FAILED_EVENT);

        // Don phai chuyen trang thai
        assertThat(order.getStatus()).isEqualTo("PAYMENT_FAILED");

        // SAGA-UC01 spec: phai co event compensation de inventory release hang.
        // Theo ADR-0001: event OrderPaymentFailed qua outbox (hoac kafka trong
        // test harness) — khong duoc im lang.
        ArgumentCaptor<String> payload = ArgumentCaptor.forClass(String.class);
        verify(kafka).send(anyString(), anyString(), payload.capture());
        JsonNode published = read(payload.getValue());
        assertThat(published.get("eventType").asText())
                .as("Phai phat event compensation khi payment fail (issue #4)")
                .isIn("OrderPaymentFailed", "OrderCancelled", "OrderPaymentCompensation");
        assertThat(published.get("orderId").asText()).isEqualTo("order-1");
    }

    @Test
    @DisplayName("SAGA-UC02: PaymentExpired cung phai co compensation")
    void paymentExpired_mustPublishCompensationEvent() {
        Order order = pendingOrder();
        when(inbox.existsByEventIdAndSource(anyString(), anyString())).thenReturn(false);
        when(orderRepository.findById("order-1")).thenReturn(Optional.of(order));

        consumer.onPaymentEvent(EXPIRED_EVENT);

        assertThat(order.getStatus()).isEqualTo("PAYMENT_FAILED");
        verify(kafka, org.mockito.Mockito.description(
                "PaymentExpired cung la fail — phai co compensating action (issue #4)"))
                .send(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("SAGA-UC03: don da o trang thai cuoi khong compensate lai (idempotent)")
    void alreadyFailedOrder_noSecondCompensation() {
        Order order = pendingOrder();
        order.setStatus("PAYMENT_FAILED");
        when(inbox.existsByEventIdAndSource(anyString(), anyString())).thenReturn(false);
        when(orderRepository.findById("order-1")).thenReturn(Optional.of(order));

        consumer.onPaymentEvent(FAILED_EVENT);

        // Event den lai khi don da FAIL: khong phat compensation thu 2
        verify(kafka, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("SAGA-UC07: status legacy trong DB khong nam trong enum -> skip idempotent, KHONG vao DLT")
    void legacyStatus_skipsInsteadOfDlt() {
        // DB co du lieu cu/dirty voi status ngoai enum (vd migration) —
        // neu fromValue throw thi event vao DLT vinh vien, don keo mai.
        // Dung hanh vi: log + skip (inbox van ghi) de consumer tiep tuc.
        Order order = pendingOrder();
        order.setStatus("LEGACY_UNKNOWN");
        when(inbox.existsByEventIdAndSource(anyString(), anyString())).thenReturn(false);
        when(orderRepository.findById("order-1")).thenReturn(Optional.of(order));

        org.junit.jupiter.api.Assertions.assertDoesNotThrow(
                () -> consumer.onPaymentEvent(FAILED_EVENT),
                "Status legacy phai skip an toan chu khong throw (review finding: DLT vinh vien)");

        // Skip van la "da xu ly": inbox ghi de event khong den lai vo han
        verify(inbox).save(any());
        // Compensation van phat: reservation co the dang HELD, listener inventory
        // idempotent nen phat thua an toan (cung ly le SAGA-UC06 order-miss)
        verify(kafka).send(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("SAGA-UC06: order khong ton tai van phat compensation — inventory tu quyet dinh")
    void orderMissing_stillPublishesCompensation() {
        // Race: event PaymentFailed den khi order chua commit / da purge.
        // Reservation van dang HELD ben inventory — compensation phai di tiep,
        // listener inventory se bo qua an toan neu khong co reservation.
        when(inbox.existsByEventIdAndSource(anyString(), anyString())).thenReturn(false);
        when(orderRepository.findById("order-1")).thenReturn(Optional.empty());

        consumer.onPaymentEvent(FAILED_EVENT);

        verify(kafka, org.mockito.Mockito.description(
                "Order miss: compensation van phai phat — inventory idempotent (code review finding)"))
                .send(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("OUTBOX-UC01 (ADR-0002, issue #5): Kafka send fail phai rethrow — khong nuot exception")
    void orderConfirmedSendFailure_mustRethrowNotSwallow() {
        Order order = pendingOrder();
        when(inbox.existsByEventIdAndSource(anyString(), anyString())).thenReturn(false);
        when(orderRepository.findById("order-1")).thenReturn(Optional.of(order));
        // kafka.send tra CompletableFuture — mock throw dong bo nhu broker chet
        when(kafka.send(anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("kafka down"));

        String paidEvent = FAILED_EVENT.replace("PaymentFailed", "PaymentSucceeded")
                .replace("\"status\":\"FAILED\"", "\"status\":\"PAID\"");

        // ADR-0002: send fail phai rethrow (transaction rollback, Kafka retry) —
        // KHONG duoc nuot exception roi ghi inbox nhu da xu ly.
        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class,
                () -> consumer.onPaymentEvent(paidEvent),
                "Send fail: phai rethrow de retry — khong duoc nuot (issue #5)");

        // Inbox KHONG duoc ghi — neu ghi thi event mat vinh vien (da "xu ly" gia)
        verify(inbox, never()).save(any());
    }

    private JsonNode read(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
