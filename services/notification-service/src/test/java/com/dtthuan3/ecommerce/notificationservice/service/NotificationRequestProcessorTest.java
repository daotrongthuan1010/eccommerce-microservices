package com.dtthuan3.ecommerce.notificationservice.service;

import com.dtthuan3.ecommerce.notificationservice.config.NotificationProperties;
import com.dtthuan3.ecommerce.notificationservice.entity.NotificationRequest;
import com.dtthuan3.ecommerce.notificationservice.entity.NotificationTemplate;
import com.dtthuan3.ecommerce.notificationservice.provider.KeycloakRecipientResolver;
import com.dtthuan3.ecommerce.notificationservice.provider.MailpitMailProvider;
import com.dtthuan3.ecommerce.notificationservice.provider.RecipientResolveResult;
import com.dtthuan3.ecommerce.notificationservice.provider.SendResult;
import com.dtthuan3.ecommerce.notificationservice.repository.NotificationDeadLetterRepository;
import com.dtthuan3.ecommerce.notificationservice.repository.NotificationDeliveryAttemptRepository;
import com.dtthuan3.ecommerce.notificationservice.repository.NotificationRequestRepository;
import com.dtthuan3.ecommerce.notificationservice.repository.NotificationTemplateRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * NOTI-05/06/07/10 — TAT CA cac case xu ly request:
 * resolve (Found/NotFound/NoEmail/Unavailable), render (thieu bien/version mat),
 * send (accepted/unknown/dlq), retry backoff, vu han, idempotent worker.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NotificationRequestProcessorTest {

    @Mock private NotificationRequestRepository requests;
    @Mock private NotificationDeliveryAttemptRepository attempts;
    @Mock private NotificationDeadLetterRepository deadLetters;
    @Mock private NotificationTemplateRepository templates;
    @Mock private MailpitMailProvider mailProvider;
    @Mock private KeycloakRecipientResolver recipientResolver;

    private NotificationProperties props;
    private SimpleMeterRegistry registry;
    private NotificationRequestProcessor processor;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        props = new NotificationProperties();
        registry = new SimpleMeterRegistry();
        processor = new NotificationRequestProcessor(requests, attempts, deadLetters,
                templates, mailProvider, recipientResolver, new TemplateService(templates),
                props, objectMapper, registry);
    }

    private NotificationRequest queuedRequest(int attemptsSoFar) {
        NotificationRequest request = new NotificationRequest();
        request.setId("req-1");
        request.setBusinessEventId("evt-1");
        request.setRecipientId("user-1");
        request.setChannel("EMAIL");
        request.setTemplateKey("order-confirmed");
        request.setTemplateVersion(1);
        request.setLocale("vi");
        request.setPurpose("TRANSACTIONAL");
        request.setBusinessKey("evt-1|user-1|EMAIL|order-confirmed");
        request.setStatus("QUEUED");
        request.setAttempts(attemptsSoFar);
        request.setNextAttemptAt(Instant.now());
        request.setOrderId("order-1");
        request.setVariables("{\"orderCode\":\"ORD-1\",\"totalAmount\":\"250,000 VND\"}");
        request.setCreatedAt(Instant.now());
        request.setUpdatedAt(Instant.now());
        return request;
    }

    private NotificationTemplate activeTemplate() {
        NotificationTemplate t = new NotificationTemplate();
        t.setTemplateKey("order-confirmed");
        t.setVersion(1);
        t.setChannel("EMAIL");
        t.setRequiredVariables("orderCode,displayName,totalAmount");
        t.setSubjectTemplate("Đơn hàng {{orderCode}} đã được xác nhận");
        t.setBodyTemplate("Xin chào {{displayName}}, đơn {{orderCode}} tổng {{totalAmount}}.");
        t.setStatus("ACTIVE");
        return t;
    }

    private void stubHappyPath(NotificationRequest request) {
        when(requests.findByIdForUpdate("req-1")).thenReturn(Optional.of(request));
        when(recipientResolver.resolve("user-1"))
                .thenReturn(new RecipientResolveResult.Found("user@example.com", "Nguyễn Văn A"));
        when(templates.findByTemplateKeyAndChannelAndVersion("order-confirmed", "EMAIL", 1))
                .thenReturn(Optional.of(activeTemplate()));
    }

    // ===== GIAI DOAN 1: RESOLVE =====

    @Test
    @DisplayName("Found: render + send thanh cong -> ACCEPTED, snapshot email/name luu lai")
    void resolveFound_sendAccepted() {
        NotificationRequest request = queuedRequest(0);
        stubHappyPath(request);
        when(mailProvider.send(anyString(), eq("user@example.com"), anyString(), anyString()))
                .thenReturn(SendResult.accepted("msg-1"));

        processor.processOne("req-1");

        assertThat(request.getStatus()).isEqualTo("ACCEPTED");
        assertThat(request.getRecipientEmail()).isEqualTo("user@example.com");
        assertThat(request.getRecipientName()).isEqualTo("Nguyễn Văn A");
        assertThat(request.getSubject()).isEqualTo("Đơn hàng ORD-1 đã được xác nhận");
        assertThat(request.getBody()).contains("Xin chào Nguyễn Văn A");
        assertThat(request.getBody()).contains("250,000 VND");
        verify(attempts).save(any());
    }

    @Test
    @DisplayName("UserNotFound: FAILED_FINAL ngay + DLQ, khong retry, khong goi SMTP")
    void resolveNotFound_permanent() {
        NotificationRequest request = queuedRequest(0);
        when(requests.findByIdForUpdate("req-1")).thenReturn(Optional.of(request));
        when(recipientResolver.resolve("user-1")).thenReturn(new RecipientResolveResult.UserNotFound());

        processor.processOne("req-1");

        assertThat(request.getStatus()).isEqualTo("FAILED_FINAL");
        assertThat(request.getAttempts()).isEqualTo(1);
        assertThat(request.getLastError()).contains("RECIPIENT_NOT_FOUND");
        verify(deadLetters).save(any());
        verify(mailProvider, never()).send(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("NoEmail (user ton tai nhung khong co email): FAILED_FINAL + DLQ, khong retry")
    void resolveNoEmail_permanent() {
        NotificationRequest request = queuedRequest(0);
        when(requests.findByIdForUpdate("req-1")).thenReturn(Optional.of(request));
        when(recipientResolver.resolve("user-1"))
                .thenReturn(new RecipientResolveResult.NoEmail("Nguyễn Văn A"));

        processor.processOne("req-1");

        assertThat(request.getStatus()).isEqualTo("FAILED_FINAL");
        assertThat(request.getLastError()).contains("RECIPIENT_NO_EMAIL");
        assertThat(request.getRecipientName()).isEqualTo("Nguyễn Văn A");
        verify(deadLetters).save(any());
    }

    @Test
    @DisplayName("Unavailable (Keycloak down/chua cau hinh): RETRYABLE voi backoff, chua vao DLQ")
    void resolveUnavailable_retry() {
        NotificationRequest request = queuedRequest(0);
        when(requests.findByIdForUpdate("req-1")).thenReturn(Optional.of(request));
        when(recipientResolver.resolve("user-1"))
                .thenReturn(new RecipientResolveResult.Unavailable("connection refused"));

        processor.processOne("req-1");

        assertThat(request.getStatus()).isEqualTo("FAILED_RETRYABLE");
        assertThat(request.getLastError()).contains("RECIPIENT_UNAVAILABLE");
        assertThat(request.getNextAttemptAt()).isAfter(Instant.now().plusSeconds(55));
        verify(deadLetters, never()).save(any());
    }

    // ===== GIAI DOAN 2: RENDER =====

    @Test
    @DisplayName("Template version da chot bien mat: FAILED_FINAL + DLQ (permanent)")
    void templateVersionMissing_permanent() {
        NotificationRequest request = queuedRequest(0);
        when(requests.findByIdForUpdate("req-1")).thenReturn(Optional.of(request));
        when(recipientResolver.resolve("user-1"))
                .thenReturn(new RecipientResolveResult.Found("user@example.com", "A"));
        when(templates.findByTemplateKeyAndChannelAndVersion(anyString(), anyString(), anyInt()))
                .thenReturn(Optional.empty());

        processor.processOne("req-1");

        assertThat(request.getStatus()).isEqualTo("FAILED_FINAL");
        assertThat(request.getLastError()).contains("TEMPLATE_VERSION_MISSING");
        verify(deadLetters).save(any());
    }

    @Test
    @DisplayName("Bien bat buoc thieu tren request: KHONG gui, FAILED_FINAL + DLQ noi dung")
    void missingVariables_permanentNoSend() {
        NotificationRequest request = queuedRequest(0);
        request.setVariables("{\"orderCode\":\"ORD-1\"}"); // thieu totalAmount
        when(requests.findByIdForUpdate("req-1")).thenReturn(Optional.of(request));
        when(recipientResolver.resolve("user-1"))
                .thenReturn(new RecipientResolveResult.Found("user@example.com", "A"));
        when(templates.findByTemplateKeyAndChannelAndVersion(anyString(), anyString(), anyInt()))
                .thenReturn(Optional.of(activeTemplate()));

        processor.processOne("req-1");

        assertThat(request.getStatus()).isEqualTo("FAILED_FINAL");
        assertThat(request.getLastError()).contains("TEMPLATE_VARIABLES_MISSING");
        // Khong bao gio goi SMTP khi chua render du
        verify(mailProvider, never()).send(anyString(), anyString(), anyString(), anyString());
        verify(deadLetters).save(any());
    }

    // ===== GIAI DOAN 3: SEND =====

    @Test
    @DisplayName("UNKNOWN + tra cuu thay businessKey: nang len DELIVERED, khong gui lai")
    void unknownRecoveredByLookup() {
        NotificationRequest request = queuedRequest(0);
        stubHappyPath(request);
        when(mailProvider.send(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(SendResult.unknown("SMTP_ERROR", "connection reset"));
        when(mailProvider.lookupByBusinessKey(request.getBusinessKey()))
                .thenReturn(SendResult.delivered("msg-1"));

        processor.processOne("req-1");

        assertThat(request.getStatus()).isEqualTo("DELIVERED");
        assertThat(request.getAttempts()).isEqualTo(1);
        verify(mailProvider).send(anyString(), anyString(), anyString(), anyString()); // dung 1 lan
    }

    @Test
    @DisplayName("UNKNOWN + tra cuu khong thay: FAILED_RETRYABLE voi backoff")
    void unknownNotFound_retry() {
        NotificationRequest request = queuedRequest(0);
        stubHappyPath(request);
        when(mailProvider.send(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(SendResult.unknown("SMTP_ERROR", "timeout"));
        when(mailProvider.lookupByBusinessKey(anyString()))
                .thenReturn(SendResult.retryable("NOT_FOUND", "khong thay"));

        processor.processOne("req-1");

        assertThat(request.getStatus()).isEqualTo("FAILED_RETRYABLE");
        assertThat(request.getNextAttemptAt()).isAfter(Instant.now());
    }

    @Test
    @DisplayName("Loi SMTP tam thuat: backoff tang dan, chua vao DLQ")
    void smtpRetryable_backoff() {
        NotificationRequest request = queuedRequest(2);
        stubHappyPath(request);
        when(mailProvider.send(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(SendResult.retryable("SMTP_5xx", "server busy"));

        processor.processOne("req-1");

        assertThat(request.getStatus()).isEqualTo("FAILED_RETRYABLE");
        assertThat(request.getAttempts()).isEqualTo(3);
        // Backoff tang dan: lan 3 = 1m << 2 = 4m (co jitter 20%)
        assertThat(request.getNextAttemptAt())
                .isAfter(Instant.now().plusSeconds(3 * 60 - 10));
        verify(deadLetters, never()).save(any());
    }

    @Test
    @DisplayName("Vu han maxAttempts (5): FAILED_FINAL + DLQ RETRY_EXHAUSTED")
    void retryExhausted_dlq() {
        NotificationRequest request = queuedRequest(5);
        stubHappyPath(request);
        when(mailProvider.send(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(SendResult.retryable("SMTP_5xx", "still down"));

        processor.processOne("req-1");

        assertThat(request.getStatus()).isEqualTo("FAILED_FINAL");
        assertThat(request.getNextAttemptAt()).isNull();
        verify(deadLetters).save(any());
    }

    @Test
    @DisplayName("Dia chi email sai (permanent tu SMTP): FAILED_FINAL ngay, 1 attempt")
    void smtpPermanent_noRetry() {
        NotificationRequest request = queuedRequest(0);
        stubHappyPath(request);
        when(mailProvider.send(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(SendResult.permanent("INVALID_ADDRESS", "bad address"));

        processor.processOne("req-1");

        assertThat(request.getStatus()).isEqualTo("FAILED_FINAL");
        assertThat(request.getAttempts()).isEqualTo(1);
        verify(deadLetters).save(any());
    }

    // ===== IDEMPOTENT / CONCURRENCY =====

    @Test
    @DisplayName("Request da DELIVERED (worker khac xu ly truoc): bo qua, khong gui lai")
    void alreadyDelivered_skipped() {
        NotificationRequest request = queuedRequest(1);
        request.setStatus("DELIVERED");
        when(requests.findByIdForUpdate("req-1")).thenReturn(Optional.of(request));

        processor.processOne("req-1");

        verify(recipientResolver, never()).resolve(anyString());
        verify(mailProvider, never()).send(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Request khong ton tai (worker khac xoa/da xu ly): bo qua yen tinh")
    void requestMissing_skipped() {
        when(requests.findByIdForUpdate("req-1")).thenReturn(Optional.empty());

        processor.processOne("req-1");

        verify(recipientResolver, never()).resolve(anyString());
        verify(deadLetters, never()).save(any());
    }

    @Test
    @DisplayName("Kenh chua ho tro (SMS): FAILED_FINAL + DLQ, khong thu lai")
    void unsupportedChannel_permanent() {
        NotificationRequest request = queuedRequest(0);
        request.setChannel("SMS");
        when(requests.findByIdForUpdate("req-1")).thenReturn(Optional.of(request));

        processor.processOne("req-1");

        assertThat(request.getStatus()).isEqualTo("FAILED_FINAL");
        verify(deadLetters).save(any());
        verify(mailProvider, never()).send(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Bien JSON tren request rac: coi nhu thieu bien -> DLQ, khong crash worker")
    void corruptVariablesJson_dlqNotCrash() {
        NotificationRequest request = queuedRequest(0);
        request.setVariables("not-json{{{");
        when(requests.findByIdForUpdate("req-1")).thenReturn(Optional.of(request));
        when(recipientResolver.resolve("user-1"))
                .thenReturn(new RecipientResolveResult.Found("user@example.com", "A"));
        when(templates.findByTemplateKeyAndChannelAndVersion(anyString(), anyString(), anyInt()))
                .thenReturn(Optional.of(activeTemplate()));

        processor.processOne("req-1");

        // Bien rac -> map rong -> thieu bien bat buoc -> permanent DLQ (khong exception)
        assertThat(request.getStatus()).isEqualTo("FAILED_FINAL");
        verify(deadLetters).save(any());
    }
}
