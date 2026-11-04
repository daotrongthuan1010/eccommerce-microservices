package com.dtthuan3.ecommerce.notificationservice.service;

import com.dtthuan3.ecommerce.notificationservice.config.NotificationProperties;
import com.dtthuan3.ecommerce.notificationservice.entity.NotificationDeadLetter;
import com.dtthuan3.ecommerce.notificationservice.entity.NotificationDeliveryAttempt;
import com.dtthuan3.ecommerce.notificationservice.entity.NotificationRequest;
import com.dtthuan3.ecommerce.notificationservice.provider.KeycloakRecipientResolver;
import com.dtthuan3.ecommerce.notificationservice.provider.MailpitMailProvider;
import com.dtthuan3.ecommerce.notificationservice.provider.SendResult;
import com.dtthuan3.ecommerce.notificationservice.repository.NotificationDeadLetterRepository;
import com.dtthuan3.ecommerce.notificationservice.repository.NotificationDeliveryAttemptRepository;
import com.dtthuan3.ecommerce.notificationservice.repository.NotificationRequestRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Xu ly 1 request trong transaction rieng (REQUIRES_NEW). Tach rieng thanh bean
 * de Spring proxy @Transactional khong bi bypass khi worker goi (self-invocation
 * khong qua proxy = khong co transaction — bug da gap khi worker goi processOne
 * ngay trong cung class).
 * NOTI-06/07: retry backoff, UNKNOWN tra cuu businessKey, vu han -> DLQ.
 */
@Component
public class NotificationRequestProcessor {

    private static final Logger log = LoggerFactory.getLogger(NotificationRequestProcessor.class);

    private final NotificationRequestRepository requests;
    private final NotificationDeliveryAttemptRepository attempts;
    private final NotificationDeadLetterRepository deadLetters;
    private final MailpitMailProvider mailProvider;
    private final KeycloakRecipientResolver recipientResolver;
    private final NotificationProperties props;
    private final MeterRegistry meterRegistry;

    public NotificationRequestProcessor(NotificationRequestRepository requests,
                                        NotificationDeliveryAttemptRepository attempts,
                                        NotificationDeadLetterRepository deadLetters,
                                        MailpitMailProvider mailProvider,
                                        KeycloakRecipientResolver recipientResolver,
                                        NotificationProperties props,
                                        MeterRegistry meterRegistry) {
        this.requests = requests;
        this.attempts = attempts;
        this.deadLetters = deadLetters;
        this.mailProvider = mailProvider;
        this.recipientResolver = recipientResolver;
        this.props = props;
        this.meterRegistry = meterRegistry;
    }

    /**
     * Xu ly 1 request: chuyen SENDING (co lock), goi provider, cap nhat ket qua.
     * Voi UNKNOWN: tra cuu businessKey truoc khi quyet dinh thu lai (NOTI-07).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processOne(String requestId) {
        NotificationRequest request = requests.findByIdForUpdate(requestId).orElse(null);
        if (request == null) return;
        // Trang thai co the da doi boi worker khac (lay batch trung) — chi xu ly khi van due
        if (!"QUEUED".equals(request.getStatus()) && !"FAILED_RETRYABLE".equals(request.getStatus())) {
            return;
        }

        Timer.Sample sample = Timer.start(meterRegistry);
        String outcome = "accepted";
        try {
            request.setStatus("SENDING");
            request.setAttempts(request.getAttempts() + 1);
            requests.saveAndFlush(request);

            SendResult result = sendWithResolver(request);

            recordAttempt(request, result);

            switch (result.outcome()) {
                case "ACCEPTED", "DELIVERED" -> {
                    request.setStatus("DELIVERED".equals(result.outcome()) ? "DELIVERED" : "ACCEPTED");
                    request.setProviderMessageId(result.providerMessageId());
                    request.setLastError(null);
                    request.setNextAttemptAt(null);
                    counter("notification.send." + result.outcome().toLowerCase()).increment();
                }
                case "UNKNOWN" -> {
                    // NOTI-07: tra provider theo businessKey truoc khi quyet dinh
                    SendResult lookup = mailProvider.lookupByBusinessKey(request.getBusinessKey());
                    if ("DELIVERED".equals(lookup.outcome())) {
                        request.setStatus("DELIVERED");
                        request.setNextAttemptAt(null);
                        outcome = "delivered";
                        counter("notification.send.unknown_recovered").increment();
                    } else {
                        scheduleRetry(request, result.errorCode(), result.errorMessage());
                        outcome = "unknown";
                        counter("notification.send.unknown").increment();
                    }
                }
                case "FAILED_RETRYABLE" -> {
                    scheduleRetry(request, result.errorCode(), result.errorMessage());
                    outcome = "retry";
                    counter("notification.send.retryable").increment();
                }
                case "FAILED_FINAL" -> {
                    request.setStatus("FAILED_FINAL");
                    request.setLastError(truncate(
                            result.errorCode() == null ? result.errorMessage()
                                    : result.errorCode() + ": " + result.errorMessage()));
                    request.setNextAttemptAt(null);
                    moveToDeadLetter(request, "PERMANENT_FAILURE", result.errorMessage());
                    outcome = "final";
                    counter("notification.send.final").increment();
                }
                default -> log.warn("Outcome khong hop le: {}", result.outcome());
            }
            requests.save(request);
        } finally {
            sample.stop(Timer.builder("notification.send.processing")
                    .tag("outcome", outcome)
                    .register(meterRegistry));
        }
    }

    /** Resolve email nguoi nhan roi goi Mailpit. Khong resolve duoc = retryable kem ly do. */
    private SendResult sendWithResolver(NotificationRequest request) {
        if (!"EMAIL".equals(request.getChannel())) {
            return SendResult.permanent("UNSUPPORTED_CHANNEL", "Kenh " + request.getChannel() + " chua ho tro");
        }
        String to = recipientResolver.resolveEmail(request.getRecipientId());
        if ("NOT_FOUND".equals(to)) {
            // User khong ton tai trong Keycloak (404): permanent, khong thu lai (NOTI-06)
            return SendResult.permanent("RECIPIENT_NOT_FOUND",
                    "User " + request.getRecipientId() + " khong ton tai trong Keycloak");
        }
        if (to == null || to.isBlank()) {
            // Chua cau hinh Keycloak admin hoac Keycloak down: thu lai sau, ly do ro rang
            return SendResult.retryable("RECIPIENT_UNRESOLVED",
                    "Khong resolve duoc email cho userId=" + request.getRecipientId());
        }
        return mailProvider.send(request.getBusinessKey(), to, request.getSubject(), request.getBody());
    }

    /** NOTI-06: backoff tang dan + jitter, gioi han maxAttempts; vu han = FAILED_FINAL + DLQ. */
    private void scheduleRetry(NotificationRequest request, String code, String message) {
        String reason = code == null ? "" : code + ": " + message;
        if (request.getAttempts() >= props.getRetry().getMaxAttempts()) {
            request.setStatus("FAILED_FINAL");
            request.setNextAttemptAt(null);
            request.setLastError(truncate(reason));
            moveToDeadLetter(request, "RETRY_EXHAUSTED", message);
            counter("notification.send.exhausted").increment();
            return;
        }
        long backoffMs = Math.min(
                props.getRetry().getInitialBackoff().toMillis() * (1L << Math.min(request.getAttempts() - 1, 10)),
                props.getRetry().getMaxBackoff().toMillis());
        long jitter = (long) (backoffMs * 0.2 * Math.random());
        request.setStatus("FAILED_RETRYABLE");
        request.setNextAttemptAt(Instant.now().plusMillis(backoffMs + jitter));
        request.setLastError(truncate(reason));
    }

    private void recordAttempt(NotificationRequest request, SendResult result) {
        NotificationDeliveryAttempt attempt = new NotificationDeliveryAttempt();
        attempt.setId(UUID.randomUUID().toString());
        attempt.setRequestId(request.getId());
        attempt.setAttemptNo(request.getAttempts());
        attempt.setOutcome(result.outcome());
        attempt.setProviderMessageId(result.providerMessageId());
        attempt.setErrorCode(result.errorCode());
        attempt.setErrorMessage(truncate(result.errorMessage()));
        attempt.setAttemptedAt(Instant.now());
        attempts.save(attempt);
    }

    private void moveToDeadLetter(NotificationRequest request, String reason, String detail) {
        NotificationDeadLetter dlq = new NotificationDeadLetter();
        dlq.setId(UUID.randomUUID().toString());
        dlq.setRequestId(request.getId());
        dlq.setReason(reason);
        dlq.setDetail(truncate(detail));
        dlq.setStatus("OPEN");
        dlq.setCreatedAt(Instant.now());
        deadLetters.save(dlq);
        log.warn("Request {} vao DLQ: {} — {}", request.getId(), reason, truncate(detail));
        counter("notification.dlq").increment();
    }

    private Counter counter(String name) {
        return meterRegistry.counter(name);
    }

    private static String truncate(String s) {
        if (s == null) return null;
        return s.length() > 500 ? s.substring(0, 500) : s;
    }
}
