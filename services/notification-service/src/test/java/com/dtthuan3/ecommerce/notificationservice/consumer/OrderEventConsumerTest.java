package com.dtthuan3.ecommerce.notificationservice.consumer;

import com.dtthuan3.ecommerce.notificationservice.entity.NotificationRequest;
import com.dtthuan3.ecommerce.notificationservice.repository.NotificationInboxRepository;
import com.dtthuan3.ecommerce.notificationservice.repository.NotificationRequestRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * NOTI-UC01/03: event OrderConfirmed den trung/khong theo thu tu khong tao request lan 2.
 * Inbox dedup theo (eventId, source); businessKey chong trung.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrderEventConsumerTest {

    private static final String CONFIRMED_EVENT = """
            {"eventId":"evt-1","eventType":"OrderConfirmed","orderId":"order-1",
             "orderCode":"ORD-20261104-ABCD","userId":"user-1","status":"CONFIRMED",
             "totalAmount":250000,"currency":"VND","occurredAt":"2026-11-04T10:00:00Z"}
            """;

    @Mock private NotificationInboxRepository inbox;
    @Mock private NotificationRequestRepository requests;

    private OrderEventConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new OrderEventConsumer(inbox, requests, new ObjectMapper());
    }

    @Test
    @DisplayName("OrderConfirmed: tao request QUEUED + luu inbox")
    void onConfirmed_createsQueuedRequest() {
        when(inbox.existsByEventIdAndSource("evt-1", "order-service")).thenReturn(false);
        when(requests.existsByBusinessKey(anyString())).thenReturn(false);

        consumer.onOrderEvent(CONFIRMED_EVENT);

        ArgumentCaptor<NotificationRequest> captor = ArgumentCaptor.forClass(NotificationRequest.class);
        verify(requests).save(captor.capture());
        NotificationRequest saved = captor.getValue();

        assertThat(saved.getStatus()).isEqualTo("QUEUED");
        assertThat(saved.getChannel()).isEqualTo("EMAIL");
        assertThat(saved.getPurpose()).isEqualTo("TRANSACTIONAL");
        assertThat(saved.getTemplateKey()).isEqualTo("order-confirmed.vi");
        assertThat(saved.getBusinessKey())
                .isEqualTo("evt-1|user-1|EMAIL|order-confirmed.vi");
        assertThat(saved.getOrderId()).isEqualTo("order-1");
        assertThat(saved.getSubject()).contains("ORD-20261104-ABCD");
        assertThat(saved.getBody()).contains("ORD-20261104-ABCD");
        verify(inbox).save(any());
    }

    @Test
    @DisplayName("Event trung (Kafka gui lai): inbox da co -> khong tao request lan 2")
    void duplicateEvent_skippedByInbox() {
        when(inbox.existsByEventIdAndSource("evt-1", "order-service")).thenReturn(true);

        consumer.onOrderEvent(CONFIRMED_EVENT);

        verify(requests, never()).save(any(NotificationRequest.class));
        verify(inbox, never()).save(any());
    }

    @Test
    @DisplayName("businessKey trung (event khac id cung noi dung): khong tao request lan 2")
    void duplicateBusinessKey_skipped() {
        when(inbox.existsByEventIdAndSource("evt-1", "order-service")).thenReturn(false);
        when(requests.existsByBusinessKey("evt-1|user-1|EMAIL|order-confirmed.vi")).thenReturn(true);

        consumer.onOrderEvent(CONFIRMED_EVENT);

        verify(requests, never()).save(any(NotificationRequest.class));
        // Inbox van duoc ghi: event nay coi nhu da xu ly
        verify(inbox).save(any());
    }

    @Test
    @DisplayName("OrderCanceled: tao request voi template order-canceled")
    void onCanceled_createsCanceledTemplate() {
        when(inbox.existsByEventIdAndSource("evt-2", "order-service")).thenReturn(false);
        when(requests.existsByBusinessKey(anyString())).thenReturn(false);

        consumer.onOrderEvent(CONFIRMED_EVENT.replace("evt-1", "evt-2")
                .replace("OrderConfirmed", "OrderCanceled"));

        ArgumentCaptor<NotificationRequest> captor = ArgumentCaptor.forClass(NotificationRequest.class);
        verify(requests).save(captor.capture());
        assertThat(captor.getValue().getTemplateKey()).isEqualTo("order-canceled.vi");
        assertThat(captor.getValue().getSubject()).contains("hủy");
    }

    @Test
    @DisplayName("Event khac (OrderCreated): khong tao thong bao, nhung van ghi inbox (da xu ly)")
    void unrelatedEvent_skipped() {
        consumer.onOrderEvent(CONFIRMED_EVENT.replace("OrderConfirmed", "OrderCreated"));

        verify(requests, never()).save(any(NotificationRequest.class));
        // Inbox van duoc ghi: event coi nhu da xu ly, event lan 2 khong phai parse lai
        verify(inbox).save(any());
    }

    @Test
    @DisplayName("Event sai shape (thieu userId): bo qua, khong crash consumer")
    void malformedEvent_swallowed() {
        consumer.onOrderEvent("{\"eventId\":\"evt-3\",\"eventType\":\"OrderConfirmed\"}");

        verify(requests, never()).save(any(NotificationRequest.class));
        verify(inbox, never()).save(any());
    }

    @Test
    @DisplayName("JSON rac: log + nuot exception, khong rethrow de khong block partition")
    void poisonMessage_swallowed() {
        consumer.onOrderEvent("not-json{{{");

        verify(requests, never()).save(any(NotificationRequest.class));
        verify(inbox, never()).save(any());
    }
}
