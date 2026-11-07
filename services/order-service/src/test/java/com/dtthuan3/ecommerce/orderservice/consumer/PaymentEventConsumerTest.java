package com.dtthuan3.ecommerce.orderservice.consumer;

import com.dtthuan3.ecommerce.orderservice.entity.Order;
import com.dtthuan3.ecommerce.orderservice.repository.OrderInboxRepository;
import com.dtthuan3.ecommerce.orderservice.repository.OrderRepository;
import com.dtthuan3.ecommerce.orderservice.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * PAY-UC01/UC02: event payment den trung/sai thu tu khong lam don nhay 2 lan.
 * Inbox dedup theo (eventId, source).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PaymentEventConsumerTest {

    private static final String PAID_EVENT = """
            {"eventId":"evt-1","eventType":"PaymentSucceeded","paymentId":"pay-1",
             "orderId":"order-1","method":"VNPAY","amount":200000,"currency":"VND",
             "status":"PAID","latePayment":false,"occurredAt":"2026-11-04T10:00:00Z"}
            """;

    @Mock private OrderInboxRepository inbox;
    @Mock private OrderRepository orderRepository;
    @Mock private KafkaTemplate<String, String> kafka;
    @Mock private OrderService orderService;

    private PaymentEventConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new PaymentEventConsumer(inbox, orderRepository, orderService, new ObjectMapper(), kafka);
    }

    private Order pendingOrder() {
        Order order = new Order();
        order.setId("order-1");
        order.setUserId("user-1");
        order.setStatus("PENDING_PAYMENT");
        return order;
    }

    @Test
    @DisplayName("PaymentSucceeded: don PENDING_PAYMENT -> PAID + luu inbox")
    void onSucceeded_marksPaid() {

        Order order = pendingOrder();
        when(inbox.existsByEventIdAndSource("evt-1", "payment-service")).thenReturn(false);
        when(orderRepository.findById("order-1")).thenReturn(Optional.of(order));

        consumer.onPaymentEvent(PAID_EVENT);

        assertThat(order.getStatus()).isEqualTo("PAID");
        assertThat(order.getPaymentId()).isEqualTo("pay-1");
        verify(orderService).publishOrderPaid(order);
        verify(inbox).save(any());
    }

    @Test
    @DisplayName("Event trung (gui lai): inbox da co -> khong xu ly lan 2")
    void duplicateEvent_skippedByInbox() {

        when(inbox.existsByEventIdAndSource("evt-1", "payment-service")).thenReturn(true);

        consumer.onPaymentEvent(PAID_EVENT);

        verify(orderRepository, never()).findById(anyString());
        verify(inbox, never()).save(any());
    }

    @Test
    @DisplayName("Event den muon (PAY-UC02 late payment): don PENDING_PAYMENT van chap nhan PAID")
    void lateSucceededEvent_stillMarksPaid() {

        Order order = pendingOrder();
        when(inbox.existsByEventIdAndSource("evt-1", "payment-service")).thenReturn(false);
        when(orderRepository.findById("order-1")).thenReturn(Optional.of(order));

        String lateEvent = PAID_EVENT.replace("\"latePayment\":false", "\"latePayment\":true");
        consumer.onPaymentEvent(lateEvent);

        assertThat(order.getStatus()).isEqualTo("PAID");
    }

    @Test
    @DisplayName("Don da PAID nhan lai event Succeeded: khong doi trang thai (idempotent)")
    void alreadyPaidOrder_ignoresDuplicateSucceeded() {

        Order order = pendingOrder();
        order.setStatus("PAID");
        when(inbox.existsByEventIdAndSource("evt-1", "payment-service")).thenReturn(false);
        when(orderRepository.findById("order-1")).thenReturn(Optional.of(order));

        consumer.onPaymentEvent(PAID_EVENT);

        // Van PAID, khong bi reset hay exception
        assertThat(order.getStatus()).isEqualTo("PAID");
        // Inbox van duoc ghi de event re-send sau nay khong xu ly lai
        verify(inbox).save(any());
    }

    @Test
    @DisplayName("PaymentExpired: don PENDING_PAYMENT -> PAYMENT_FAILED")
    void onExpired_marksPaymentFailed() {

        Order order = pendingOrder();
        when(inbox.existsByEventIdAndSource("evt-2", "payment-service")).thenReturn(false);
        when(orderRepository.findById("order-1")).thenReturn(Optional.of(order));

        String expired = PAID_EVENT
                .replace("\"eventId\":\"evt-1\"", "\"eventId\":\"evt-2\"")
                .replace("\"eventType\":\"PaymentSucceeded\"", "\"eventType\":\"PaymentExpired\"");

        consumer.onPaymentEvent(expired);

        assertThat(order.getStatus()).isEqualTo("PAYMENT_FAILED");
    }

    @Test
    @DisplayName("Event sai shape (thieu field): bo qua, khong crash consumer")
    void malformedEvent_skipped() {

        consumer.onPaymentEvent("{\"eventType\":\"PaymentSucceeded\"}");

        verify(orderRepository, never()).findById(anyString());
        verify(inbox, never()).save(any());
    }
}
