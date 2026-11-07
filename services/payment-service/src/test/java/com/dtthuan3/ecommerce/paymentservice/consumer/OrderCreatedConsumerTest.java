package com.dtthuan3.ecommerce.paymentservice.consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dtthuan3.ecommerce.paymentservice.domain.PaymentStatus;
import com.dtthuan3.ecommerce.paymentservice.dto.request.CreatePaymentRequest;
import com.dtthuan3.ecommerce.paymentservice.entity.Payment;
import com.dtthuan3.ecommerce.paymentservice.repository.PaymentRepository;
import com.dtthuan3.ecommerce.paymentservice.service.PaymentService;
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

/**
 * PAY-UC01..05: consumer Kafka order-events cua payment-service — mat xich
 * tien cua saga checkout. Gap P0: khong co test truoc day; consumer nay
 * neu bug thi don thanh toan khong bao gio co payment.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrderCreatedConsumerTest {

    private static final String ORDER_CREATED = """
            {"eventId":"evt-1","eventType":"OrderCreated","orderId":"order-1",
             "orderCode":"ORD-001","userId":"user-1","status":"PENDING_PAYMENT",
             "totalAmount":250000,"currency":"VND","paymentMethod":"VNPAY",
             "occurredAt":"2026-11-07T10:00:00Z"}
            """;

    @Mock private PaymentService paymentService;
    @Mock private PaymentRepository payments;

    private OrderCreatedConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new OrderCreatedConsumer(paymentService, payments, new ObjectMapper());
    }

    @Test
    @DisplayName("PAY-UC01: OrderCreated VNPAY -> tao payment voi Idempotency-Key = order-{orderId}")
    void orderCreated_createsPaymentWithStableKey() {
        when(payments.findFirstByOrderIdOrderByCreatedAtDesc("order-1")).thenReturn(Optional.empty());

        consumer.onOrderCreated(ORDER_CREATED);

        ArgumentCaptor<CreatePaymentRequest> req = ArgumentCaptor.forClass(CreatePaymentRequest.class);
        // Key on dinh theo orderId — event den lai khong tao payment thu 2
        verify(paymentService).create(req.capture(), eq("order-order-1"), anyString());
        assertThat(req.getValue().orderId()).isEqualTo("order-1");
        assertThat(req.getValue().amount()).isEqualTo(250_000L);
        assertThat(req.getValue().method().name()).isEqualTo("VNPAY");
    }

    @Test
    @DisplayName("PAY-UC02: don COD khong tao payment online (thu tien khi giao hang)")
    void codOrder_skipsPaymentCreation() {
        String cod = ORDER_CREATED.replace("\"VNPAY\"", "\"COD\"");

        consumer.onOrderCreated(cod);

        verify(paymentService, never()).create(any(), anyString(), anyString());
    }

    @Test
    @DisplayName("PAY-UC03: event den lai (da co payment) -> bo qua, khong tao thu 2")
    void duplicateEvent_skipsWhenPaymentExists() {
        Payment existing = new Payment();
        existing.setOrderId("order-1");
        existing.setStatus(PaymentStatus.PENDING);
        when(payments.findFirstByOrderIdOrderByCreatedAtDesc("order-1"))
                .thenReturn(Optional.of(existing));

        consumer.onOrderCreated(ORDER_CREATED);

        verify(paymentService, never()).create(any(), anyString(), anyString());
    }

    @Test
    @DisplayName("PAY-UC04: totalAmount <= 0 -> bo qua (khong tao payment so tien am)")
    void invalidAmount_skips() {
        String bad = ORDER_CREATED.replace("\"totalAmount\":250000", "\"totalAmount\":0");

        consumer.onOrderCreated(bad);

        verify(paymentService, never()).create(any(), anyString(), anyString());
    }

    @Test
    @DisplayName("PAY-UC05: event khac (OrderConfirmed...) + JSON hong -> bo qua an toan")
    void unrelatedOrMalformedEvent_skips() {
        consumer.onOrderCreated(ORDER_CREATED.replace("OrderCreated", "OrderConfirmed"));
        consumer.onOrderCreated("not-json{{{");

        verify(paymentService, never()).create(any(), anyString(), anyString());
        verify(payments, never()).findFirstByOrderIdOrderByCreatedAtDesc(anyString());
    }
}
