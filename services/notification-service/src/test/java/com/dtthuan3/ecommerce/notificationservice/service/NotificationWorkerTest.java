package com.dtthuan3.ecommerce.notificationservice.service;

import com.dtthuan3.ecommerce.notificationservice.config.NotificationProperties;
import com.dtthuan3.ecommerce.notificationservice.entity.NotificationRequest;
import com.dtthuan3.ecommerce.notificationservice.provider.KeycloakRecipientResolver;
import com.dtthuan3.ecommerce.notificationservice.provider.MailpitMailProvider;
import com.dtthuan3.ecommerce.notificationservice.provider.SendResult;
import com.dtthuan3.ecommerce.notificationservice.repository.NotificationDeadLetterRepository;
import com.dtthuan3.ecommerce.notificationservice.repository.NotificationDeliveryAttemptRepository;
import com.dtthuan3.ecommerce.notificationservice.repository.NotificationRequestRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Instant;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * NOTI-06/07/10: retry backoff co gioi han, UNKNOWN tra provider truoc thu lai,
 * vu han maxAttempts -> FAILED_FINAL + DLQ; mail loi khong anh huong don (processor doc lap).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NotificationWorkerTest {

    @Mock private NotificationRequestRepository requests;
    @Mock private NotificationDeliveryAttemptRepository attempts;
    @Mock private NotificationDeadLetterRepository deadLetters;
    @Mock private MailpitMailProvider mailProvider;
    @Mock private KeycloakRecipientResolver recipientResolver;

    private NotificationProperties props;
    private SimpleMeterRegistry registry;
    private NotificationRequestProcessor processor;

    @BeforeEach
    void setUp() {
        props = new NotificationProperties();
        registry = new SimpleMeterRegistry();
        processor = new NotificationRequestProcessor(requests, attempts, deadLetters,
                mailProvider, recipientResolver, props, registry);
    }

    private NotificationRequest queuedRequest(int attemptsSoFar) {
        NotificationRequest request = new NotificationRequest();
        request.setId("req-1");
        request.setBusinessEventId("evt-1");
        request.setRecipientId("user-1");
        request.setChannel("EMAIL");
        request.setTemplateKey("order-confirmed.vi");
        request.setPurpose("TRANSACTIONAL");
        request.setBusinessKey("evt-1|user-1|EMAIL|order-confirmed.vi");
        request.setSubject("Test subject");
        request.setBody("Test body");
        request.setStatus("QUEUED");
        request.setAttempts(attemptsSoFar);
        request.setNextAttemptAt(Instant.now());
        request.setOrderId("order-1");
        request.setCreatedAt(Instant.now());
        request.setUpdatedAt(Instant.now());
        return request;
    }

    @Test
    @DisplayName("Gui thanh cong: status ACCEPTED, attempt duoc ghi")
    void sendAccepted() {
        NotificationRequest request = queuedRequest(0);
        when(requests.findByIdForUpdate("req-1")).thenReturn(Optional.of(request));
        when(recipientResolver.resolveEmail("user-1")).thenReturn("user@example.com");
        when(mailProvider.send(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(SendResult.accepted("msg-1"));

        processor.processOne("req-1");

        assertThat(request.getStatus()).isEqualTo("ACCEPTED");
        assertThat(request.getProviderMessageId()).isEqualTo("msg-1");
        assertThat(request.getNextAttemptAt()).isNull();
        verify(attempts).save(any());
    }

    @Test
    @DisplayName("UNKNOWN (Mailpit co the da nhan): tra cuu businessKey thay vi gui lai mù")
    void unknownLooksUpBeforeRetry() {
        NotificationRequest request = queuedRequest(0);
        when(requests.findByIdForUpdate("req-1")).thenReturn(Optional.of(request));
        when(recipientResolver.resolveEmail("user-1")).thenReturn("user@example.com");
        when(mailProvider.send(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(SendResult.unknown("SMTP_ERROR", "connection reset"));
        // Tra cuu thay Mailpit da nhan -> nang len DELIVERED, khong gui lai
        when(mailProvider.lookupByBusinessKey(request.getBusinessKey()))
                .thenReturn(SendResult.delivered("msg-1"));

        processor.processOne("req-1");

        assertThat(request.getStatus()).isEqualTo("DELIVERED");
        assertThat(request.getAttempts()).isEqualTo(1);
        // Khong gui mail lan 2 — chi 1 lan send duy nhat
        verify(mailProvider).send(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("UNKNOWN + tra cuu khong thay: len lich thu lai voi backoff, khong gui lai ngay")
    void unknownNotFound_schedulesRetry() {
        NotificationRequest request = queuedRequest(0);
        when(requests.findByIdForUpdate("req-1")).thenReturn(Optional.of(request));
        when(recipientResolver.resolveEmail("user-1")).thenReturn("user@example.com");
        when(mailProvider.send(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(SendResult.unknown("SMTP_ERROR", "timeout"));
        when(mailProvider.lookupByBusinessKey(anyString()))
                .thenReturn(SendResult.retryable("NOT_FOUND", "khong thay"));

        processor.processOne("req-1");

        assertThat(request.getStatus()).isEqualTo("FAILED_RETRYABLE");
        assertThat(request.getNextAttemptAt()).isAfter(Instant.now());
        verify(mailProvider).send(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Loi tam thuat: backoff tang dan, attempts tang, chua vao DLQ")
    void retryableSchedulesBackoff() {
        NotificationRequest request = queuedRequest(1);
        when(requests.findByIdForUpdate("req-1")).thenReturn(Optional.of(request));
        when(recipientResolver.resolveEmail("user-1")).thenReturn("user@example.com");
        when(mailProvider.send(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(SendResult.retryable("SMTP_5xx", "server busy"));

        processor.processOne("req-1");

        assertThat(request.getStatus()).isEqualTo("FAILED_RETRYABLE");
        assertThat(request.getAttempts()).isEqualTo(2);
        // Backoff >= initial backoff (1m) — khong gui lai ngay lap tuc
        assertThat(request.getNextAttemptAt())
                .isAfter(Instant.now().plusSeconds(55));
        verify(deadLetters, never()).save(any());
    }

    @Test
    @DisplayName("Vu han maxAttempts (5): FAILED_FINAL + vao DLQ, khong thu lai nua")
    void exhaustedMovesToDlq() {
        NotificationRequest request = queuedRequest(5); // da thu 5 lan
        when(requests.findByIdForUpdate("req-1")).thenReturn(Optional.of(request));
        when(recipientResolver.resolveEmail("user-1")).thenReturn("user@example.com");
        when(mailProvider.send(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(SendResult.retryable("SMTP_5xx", "still down"));

        processor.processOne("req-1");

        assertThat(request.getStatus()).isEqualTo("FAILED_FINAL");
        assertThat(request.getNextAttemptAt()).isNull();
        verify(deadLetters).save(any());
    }

    @Test
    @DisplayName("Dia chi sai (permanent): FAILED_FINAL ngay, khong thu lai (NOTI-06)")
    void permanentFailureNoRetry() {
        NotificationRequest request = queuedRequest(0);
        when(requests.findByIdForUpdate("req-1")).thenReturn(Optional.of(request));
        when(recipientResolver.resolveEmail("user-1")).thenReturn("khong-dung@@@");
        when(mailProvider.send(anyString(), eq("khong-dung@@@"), anyString(), anyString()))
                .thenReturn(SendResult.permanent("INVALID_ADDRESS", "bad address"));

        processor.processOne("req-1");

        assertThat(request.getStatus()).isEqualTo("FAILED_FINAL");
        assertThat(request.getAttempts()).isEqualTo(1);
        verify(deadLetters).save(any());
    }

    @Test
    @DisplayName("Khong resolve duoc email: retryable kem ly do, khong gui mù")
    void unresolvedRecipientRetries() {
        NotificationRequest request = queuedRequest(0);
        when(requests.findByIdForUpdate("req-1")).thenReturn(Optional.of(request));
        when(recipientResolver.resolveEmail("user-1")).thenReturn(null);

        processor.processOne("req-1");

        assertThat(request.getStatus()).isEqualTo("FAILED_RETRYABLE");
        assertThat(request.getLastError()).contains("RECIPIENT_UNRESOLVED");
        verify(mailProvider, never()).send(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("User khong ton tai Keycloak (404): FAILED_FINAL ngay, khong thu lai 5 lan")
    void recipientNotFoundPermanent() {
        NotificationRequest request = queuedRequest(0);
        when(requests.findByIdForUpdate("req-1")).thenReturn(Optional.of(request));
        when(recipientResolver.resolveEmail("user-1")).thenReturn("NOT_FOUND");

        processor.processOne("req-1");

        assertThat(request.getStatus()).isEqualTo("FAILED_FINAL");
        assertThat(request.getLastError()).contains("RECIPIENT_NOT_FOUND");
        verify(deadLetters).save(any());
        // Khong bao gio goi SMTP cho user khong ton tai
        verify(mailProvider, never()).send(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Request da DELIVERED (worker khac xu ly truoc): bo qua, khong gui lai")
    void alreadyDeliveredSkipped() {
        NotificationRequest request = queuedRequest(1);
        request.setStatus("DELIVERED");
        when(requests.findByIdForUpdate("req-1")).thenReturn(Optional.of(request));

        processor.processOne("req-1");

        verify(mailProvider, never()).send(anyString(), anyString(), anyString(), anyString());
        verify(attempts, never()).save(any());
    }

    @Test
    @DisplayName("Template render: bien {{orderCode}} duoc thay, request luu noi dung dung thoi diem")
    void templateRendering() {
        String body = TemplateRenderer.render(
                "Don hang {{orderCode}} tong {{totalAmount}}",
                java.util.Map.of("orderCode", "ORD-1", "totalAmount", "250,000 VND"));
        assertThat(body).isEqualTo("Don hang ORD-1 tong 250,000 VND");
    }

    @Test
    @DisplayName("Bien khong co gia tri: giu nguyen placeholder de nhan vien thay (khong gui sai)")
    void templateMissingVarKept() {
        String body = TemplateRenderer.render("Xin chao {{ten}}", java.util.Map.of());
        assertThat(body).isEqualTo("Xin chao {{ten}}");
    }
}
