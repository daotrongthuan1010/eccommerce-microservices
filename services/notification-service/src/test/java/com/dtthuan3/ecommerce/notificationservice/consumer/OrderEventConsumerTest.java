package com.dtthuan3.ecommerce.notificationservice.consumer;

import com.dtthuan3.ecommerce.notificationservice.entity.NotificationRequest;
import com.dtthuan3.ecommerce.notificationservice.entity.NotificationTemplate;
import com.dtthuan3.ecommerce.notificationservice.repository.NotificationDeadLetterRepository;
import com.dtthuan3.ecommerce.notificationservice.repository.NotificationInboxRepository;
import com.dtthuan3.ecommerce.notificationservice.repository.NotificationRequestRepository;
import com.dtthuan3.ecommerce.notificationservice.repository.NotificationTemplateRepository;
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
 * Consumer chi chot du lieu (templateVersion + bien), khong render khong gui.
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
    @Mock private NotificationTemplateRepository templates;
    @Mock private NotificationDeadLetterRepository deadLetters;

    private OrderEventConsumer consumer;

    private NotificationTemplate activeTemplate(String key) {
        NotificationTemplate t = new NotificationTemplate();
        t.setTemplateKey(key);
        t.setVersion(3); // version moi nhat de kiem tra consumer chot dung
        t.setChannel("EMAIL");
        t.setLocale("vi");
        t.setPurpose("TRANSACTIONAL");
        t.setRequiredVariables("orderCode,displayName,totalAmount");
        t.setStatus("ACTIVE");
        return t;
    }

    @BeforeEach
    void setUp() {
        consumer = new OrderEventConsumer(inbox, requests, templates, deadLetters, new ObjectMapper());
        when(templates.findFirstByTemplateKeyAndChannelAndLocaleAndStatusOrderByVersionDesc(
                anyString(), anyString(), anyString(), anyString()))
                .thenReturn(Optional.of(activeTemplate("order-confirmed")));
    }

    private NotificationRequest captureSaved() {
        ArgumentCaptor<NotificationRequest> captor = ArgumentCaptor.forClass(NotificationRequest.class);
        verify(requests).save(captor.capture());
        return captor.getValue();
    }

    @Test
    @DisplayName("OrderConfirmed: tao request QUEUED chot templateVersion=3 + bien, KHONG render")
    void onConfirmed_createsRequestWithSnapshot() {
        when(inbox.existsByEventIdAndSource("evt-1", "order-service")).thenReturn(false);
        when(requests.existsByBusinessKey(anyString())).thenReturn(false);

        consumer.onOrderEvent(CONFIRMED_EVENT);

        NotificationRequest saved = captureSaved();
        assertThat(saved.getStatus()).isEqualTo("QUEUED");
        assertThat(saved.getTemplateKey()).isEqualTo("order-confirmed");
        assertThat(saved.getTemplateVersion()).isEqualTo(3); // chot version ACTIVE moi nhat
        assertThat(saved.getLocale()).isEqualTo("vi");
        assertThat(saved.getBusinessKey()).isEqualTo("evt-1|user-1|EMAIL|order-confirmed");
        assertThat(saved.getVariables()).contains("ORD-20261104-ABCD").contains("250.000 VND");
        // Consumer KHONG render — subject/body null cho den khi worker gui
        assertThat(saved.getSubject()).isNull();
        assertThat(saved.getBody()).isNull();
        assertThat(saved.getPurpose()).isEqualTo("TRANSACTIONAL");
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
    @DisplayName("businessKey trung: khong tao request lan 2, van ghi inbox")
    void duplicateBusinessKey_skipped() {
        when(inbox.existsByEventIdAndSource("evt-1", "order-service")).thenReturn(false);
        when(requests.existsByBusinessKey("evt-1|user-1|EMAIL|order-confirmed")).thenReturn(true);

        consumer.onOrderEvent(CONFIRMED_EVENT);

        verify(requests, never()).save(any(NotificationRequest.class));
        verify(inbox).save(any());
    }

    @Test
    @DisplayName("OrderCanceled: template order-canceled, purpose van TRANSACTIONAL")
    void onCanceled_usesCanceledTemplate() {
        when(inbox.existsByEventIdAndSource("evt-2", "order-service")).thenReturn(false);
        when(requests.existsByBusinessKey(anyString())).thenReturn(false);
        when(templates.findFirstByTemplateKeyAndChannelAndLocaleAndStatusOrderByVersionDesc(
                eq("order-canceled"), anyString(), anyString(), anyString()))
                .thenReturn(Optional.of(activeTemplate("order-canceled")));

        consumer.onOrderEvent(CONFIRMED_EVENT.replace("evt-1", "evt-2")
                .replace("OrderConfirmed", "OrderCanceled"));

        NotificationRequest saved = captureSaved();
        assertThat(saved.getTemplateKey()).isEqualTo("order-canceled");
    }

    private static String eq(String s) { return org.mockito.ArgumentMatchers.eq(s); }

    @Test
    @DisplayName("Template ACTIVE khong ton tai: KHONG tao request, vao DLQ TEMPLATE_MISSING")
    void templateMissing_dlq() {
        when(inbox.existsByEventIdAndSource("evt-1", "order-service")).thenReturn(false);
        when(templates.findFirstByTemplateKeyAndChannelAndLocaleAndStatusOrderByVersionDesc(
                anyString(), anyString(), anyString(), anyString()))
                .thenReturn(Optional.empty());

        consumer.onOrderEvent(CONFIRMED_EVENT);

        verify(requests, never()).save(any(NotificationRequest.class));
        verify(deadLetters).save(any());
    }

    @Test
    @DisplayName("Event khac (OrderCreated): khong tao thong bao, van ghi inbox")
    void unrelatedEvent_skipped() {
        when(inbox.existsByEventIdAndSource("evt-1", "order-service")).thenReturn(false);

        consumer.onOrderEvent(CONFIRMED_EVENT.replace("OrderConfirmed", "OrderCreated"));

        verify(requests, never()).save(any(NotificationRequest.class));
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
