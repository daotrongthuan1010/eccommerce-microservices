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
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Worker nen: quet request due (QUEUED/FAILED_RETRYABLE) va gui qua Mailpit.
 * NOTI-01: gui nam ngoai transaction DB; NOTI-06: backoff + gioi han 5 lan;
 * NOTI-07: UNKNOWN tra provider bang businessKey truoc khi thu lai.
 * Don confirmed khong bao gio bi rollback vi mail loi (mail la phu).
 */
@Component
public class NotificationWorker {

    private static final Logger log = LoggerFactory.getLogger(NotificationWorker.class);

    private final NotificationRequestRepository requests;
    private final NotificationDeliveryAttemptRepository attempts;
    private final NotificationDeadLetterRepository deadLetters;
    private final MailpitMailProvider mailProvider;
    private final KeycloakRecipientResolver recipientResolver;
    private final NotificationProperties props;
    private final MeterRegistry meterRegistry;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread pollerThread;
    private ExecutorService sendPool;

    public NotificationWorker(NotificationRequestRepository requests,
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

    @PostConstruct
    void start() {
        running.set(true);
        sendPool = Executors.newFixedThreadPool(props.getWorker().getConcurrency());
        pollerThread = new Thread(this::pollLoop, "notification-worker");
        pollerThread.setDaemon(true);
        pollerThread.start();
        log.info("NotificationWorker bat dau: batch={} concurrency={} poll={}ms",
                props.getWorker().getBatchSize(), props.getWorker().getConcurrency(),
                props.getWorker().getPollIntervalMs());
    }

    @PreDestroy
    void stop() throws InterruptedException {
        running.set(false);
        if (pollerThread != null) pollerThread.join(5000);
        if (sendPool != null) {
            sendPool.shutdown();
            sendPool.awaitTermination(10, TimeUnit.SECONDS);
        }
    }

    private void pollLoop() {
        while (running.get()) {
            try {
                int processed = pollOnce();
                if (processed == 0) {
                    // Khong co gi due — ngu mot chu ky thay vi quay vong an CPU
                    Thread.sleep(props.getWorker().getPollIntervalMs());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                log.error("Worker poll loop loi: {}", e.getMessage(), e);
                try { Thread.sleep(props.getWorker().getPollIntervalMs()); }
                catch (InterruptedException ie) { Thread.currentThread().interrupt(); return; }
            }
        }
    }

    /** Mot vong quet: lay batch due va dispatch song song cho sendPool. */
    int pollOnce() {
        List<NotificationRequest> due = requests.findDueBatch(Instant.now(), props.getWorker().getBatchSize());
        // Gioi han batch tai Java de tranh doc qua nhieu (findDueBatch khong limit trong SQL de don gian)
        if (due.size() > props.getWorker().getBatchSize()) {
            due = due.subList(0, props.getWorker().getBatchSize());
        }
        if (due.isEmpty()) return 0;
        log.debug("Quet duoc {} request due", due.size());
        for (NotificationRequest request : due) {
            sendPool.submit(() -> {
                try {
                    processOne(request.getId());
                } catch (Exception e) {
                    log.error("Xu ly request {} loi: {}", request.getId(), e.getMessage(), e);
                }
            });
        }
        return due.size();
    }

    /**
     * Xu ly 1 request trong transaction rieng: chuyen SENDING (co lock), goi provider,
     * cap nhat ket qua. Voi UNKNOWN: tra cuu businessKey truoc khi quyet dinh thu lai.
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
                    request.setLastError(truncate(result.errorMessage()));
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
        if (to == null || to.isBlank()) {
            // Chua cau hinh Keycloak admin hoac user khong co email: thu lai sau, ly do ro rang
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
