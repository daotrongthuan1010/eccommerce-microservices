package com.dtthuan3.ecommerce.notificationservice.service;

import com.dtthuan3.ecommerce.notificationservice.config.NotificationProperties;
import com.dtthuan3.ecommerce.notificationservice.entity.NotificationDeadLetter;
import com.dtthuan3.ecommerce.notificationservice.entity.NotificationDeliveryAttempt;
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
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Xu ly 1 request trong transaction rieng (REQUIRES_NEW, bean rieng de proxy
 * @Transactional khong bi self-invocation bypass).
 *
 * Pipeline 3 giai doan, moi giai doan co case loi rieng (NOTI-05/06):
 *  1. RESOLVE nguoi nhan  : Found / UserNotFound (permanent) / NoEmail (permanent) / Unavailable (retry)
 *  2. RENDER template     : version da chot + bien da chot; MissingVariables (permanent), template mat (permanent)
 *  3. SEND qua Mailpit    : ACCEPTED / DELIVERED / FAILED_RETRYABLE / FAILED_FINAL / UNKNOWN
 *                           UNKNOWN -> tra cuu businessKey truoc khi quyet dinh (NOTI-07)
 * Retry backoff + jitter, toi da maxAttempts; vu han -> FAILED_FINAL + DLQ (NOTI-06).
 * Mail loi KHONG anh huong don hang — don van CONFIRMED (NOTI-01).
 */
@Component
public class NotificationRequestProcessor {

    private static final Logger log = LoggerFactory.getLogger(NotificationRequestProcessor.class);

    private final NotificationRequestRepository requests;
    private final NotificationDeliveryAttemptRepository attempts;
    private final NotificationDeadLetterRepository deadLetters;
    private final NotificationTemplateRepository templates;
    private final MailpitMailProvider mailProvider;
    private final KeycloakRecipientResolver recipientResolver;
    private final TemplateService templateService;
    private final NotificationProperties props;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    public NotificationRequestProcessor(NotificationRequestRepository requests,
                                        NotificationDeliveryAttemptRepository attempts,
                                        NotificationDeadLetterRepository deadLetters,
                                        NotificationTemplateRepository templates,
                                        MailpitMailProvider mailProvider,
                                        KeycloakRecipientResolver recipientResolver,
                                        TemplateService templateService,
                                        NotificationProperties props,
                                        ObjectMapper objectMapper,
                                        MeterRegistry meterRegistry) {
        this.requests = requests;
        this.attempts = attempts;
        this.deadLetters = deadLetters;
        this.templates = templates;
        this.mailProvider = mailProvider;
        this.recipientResolver = recipientResolver;
        this.templateService = templateService;
        this.props = props;
        this.objectMapper = objectMapper;
        this.meterRegistry = meterRegistry;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processOne(String requestId) {
        NotificationRequest request = requests.findByIdForUpdate(requestId).orElse(null);
        if (request == null) return;
        // Worker khac co the da xu ly (batch trung) — chi xu ly khi van due
        if (!"QUEUED".equals(request.getStatus()) && !"FAILED_RETRYABLE".equals(request.getStatus())) {
            return;
        }

        Timer.Sample sample = Timer.start(meterRegistry);
        String outcome = "accepted";
        try {
            request.setAttempts(request.getAttempts() + 1);
            requests.saveAndFlush(request);

            // === Giai doan 0: kenh phai duoc ho tro ===
            if (!"EMAIL".equals(request.getChannel())) {
                finishPermanent(request, outcome, "UNSUPPORTED_CHANNEL",
                        "Kenh " + request.getChannel() + " chua ho tro", "UNSUPPORTED_CHANNEL");
                return;
            }

            // === Giai doan 1: RESOLVE nguoi nhan ===
            RecipientResolveResult resolved = recipientResolver.resolve(request.getRecipientId());
            if (resolved instanceof RecipientResolveResult.UserNotFound) {
                request.setRecipientName(null);
                request.setRecipientEmail(null);
                finishPermanent(request, outcome, "RECIPIENT_NOT_FOUND",
                        "User " + request.getRecipientId() + " khong ton tai trong Keycloak",
                        "RECIPIENT_NOT_FOUND");
                return;
            }
            if (resolved instanceof RecipientResolveResult.NoEmail noEmail) {
                request.setRecipientName(noEmail.displayName());
                request.setRecipientEmail(null);
                finishPermanent(request, outcome, "RECIPIENT_NO_EMAIL",
                        "User " + request.getRecipientId() + " khong co email trong Keycloak",
                        "RECIPIENT_NO_EMAIL");
                return;
            }
            if (resolved instanceof RecipientResolveResult.Unavailable unavailable) {
                scheduleRetry(request, "RECIPIENT_UNAVAILABLE",
                        "Khong resolve duoc email userId=" + request.getRecipientId()
                                + ": " + unavailable.reason());
                outcome = "retry";
                counter("notification.send.retryable").increment();
                return;
            }
            RecipientResolveResult.Found found = (RecipientResolveResult.Found) resolved;
            request.setRecipientEmail(found.email());
            request.setRecipientName(found.displayName());

            // === Giai doan 2: RENDER template theo version da chot ===
            SendResult result = renderAndSend(request, found);
            if (result == null) {
                return; // da xu ly o trong (permanent/retry da ghi)
            }

            // === Giai doan 3: XU LY ket qua send ===
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
                    // NOTI-07: tra provider theo businessKey truoc khi quyet dinh thu lai
                    SendResult lookup = mailProvider.lookupByBusinessKey(request.getBusinessKey());
                    if ("DELIVERED".equals(lookup.outcome())) {
                        request.setStatus("DELIVERED");
                        request.setNextAttemptAt(null);
                        request.setLastError(null);
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
                    finishPermanent(request, outcome, result.errorCode(), result.errorMessage(), "PERMANENT_FAILURE");
                }
                default -> log.warn("Outcome khong hop le: {}", result.outcome());
            }
        } finally {
            sample.stop(Timer.builder("notification.send.processing")
                    .tag("outcome", outcome)
                    .register(meterRegistry));
        }
    }

    /**
     * Giai doan 2+3: render template version da chot roi gui Mailpit.
     * Tra SendResult cua lan send; null neu da xu ly loi render (permanent).
     */
    private SendResult renderAndSend(NotificationRequest request, RecipientResolveResult.Found found) {
        NotificationTemplate template = templates
                .findByTemplateKeyAndChannelAndVersion(
                        request.getTemplateKey(), request.getChannel(), request.getTemplateVersion())
                .orElse(null);
        if (template == null) {
            // Version da chot bien mat (rollback migration?) — permanent, khong retry
            finishPermanent(request, "final", "TEMPLATE_VERSION_MISSING",
                    "Template " + request.getTemplateKey() + " v" + request.getTemplateVersion()
                            + " khong ton tai",
                    "TEMPLATE_VERSION_MISSING");
            return null;
        }

        Map<String, Object> variables = readVariables(request);
        variables.put("displayName", found.displayName());
        TemplateService.RenderResult rendered = templateService.render(template, variables);
        if (rendered instanceof TemplateService.RenderResult.MissingVariables missing) {
            // NOTI-UC04 nhanh 1: bien thieu khong gui — vao DLQ noi dung
            finishPermanent(request, "final", "TEMPLATE_VARIABLES_MISSING",
                    "Bien thieu: " + String.join(", ", missing.missing()),
                    "TEMPLATE_VARIABLES_MISSING");
            return null;
        }
        TemplateService.RenderResult.Rendered ok = (TemplateService.RenderResult.Rendered) rendered;
        request.setSubject(ok.subject());
        request.setBody(ok.body());

        return mailProvider.send(request.getBusinessKey(), found.email(), ok.subject(), ok.body());
    }

    /** Bien da chot tren request (JSON) + displayName vua resolve. */
    private Map<String, Object> readVariables(NotificationRequest request) {
        Map<String, Object> variables = new HashMap<>();
        if (request.getVariables() != null && !request.getVariables().isBlank()) {
            try {
                variables.putAll(objectMapper.readValue(request.getVariables(),
                        new TypeReference<Map<String, Object>>() {}));
            } catch (Exception e) {
                log.warn("Bien request {} khong doc duoc JSON: {}", request.getId(), e.getMessage());
            }
        }
        return variables;
    }

    /** Ket thuc permanent: FAILED_FINAL + DLQ kem ly do ro rang. */
    private void finishPermanent(NotificationRequest request, String outcome,
                                 String errorCode, String errorMessage, String dlqReason) {
        request.setStatus("FAILED_FINAL");
        request.setNextAttemptAt(null);
        request.setLastError(truncate(errorCode + ": " + errorMessage));
        moveToDeadLetter(request, dlqReason, errorMessage);
        outcome = "final";
        counter("notification.send.final").increment();
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
