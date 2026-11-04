package com.dtthuan3.ecommerce.paymentservice.service;

import com.dtthuan3.ecommerce.common.exception.BusinessException;
import com.dtthuan3.ecommerce.common.exception.ErrorCode;
import com.dtthuan3.ecommerce.common.exception.ResourceNotFoundException;
import com.dtthuan3.ecommerce.paymentservice.config.PaymentProperties;
import com.dtthuan3.ecommerce.paymentservice.domain.PaymentMethod;
import com.dtthuan3.ecommerce.paymentservice.domain.PaymentStatus;
import com.dtthuan3.ecommerce.paymentservice.domain.RefundStatus;
import com.dtthuan3.ecommerce.paymentservice.dto.request.CreatePaymentRequest;
import com.dtthuan3.ecommerce.paymentservice.dto.request.CreateRefundRequest;
import com.dtthuan3.ecommerce.paymentservice.dto.request.ProviderCallbackRequest;
import com.dtthuan3.ecommerce.paymentservice.dto.response.PaymentResponse;
import com.dtthuan3.ecommerce.paymentservice.dto.response.RefundResponse;
import com.dtthuan3.ecommerce.paymentservice.entity.Payment;
import com.dtthuan3.ecommerce.paymentservice.entity.PaymentCallbackLog;
import com.dtthuan3.ecommerce.paymentservice.entity.PaymentOutbox;
import com.dtthuan3.ecommerce.paymentservice.entity.PaymentRefund;
import com.dtthuan3.ecommerce.paymentservice.provider.PaymentProvider;
import com.dtthuan3.ecommerce.paymentservice.repository.PaymentCallbackLogRepository;
import com.dtthuan3.ecommerce.paymentservice.repository.PaymentOutboxRepository;
import com.dtthuan3.ecommerce.paymentservice.repository.PaymentRefundRepository;
import com.dtthuan3.ecommerce.paymentservice.repository.PaymentRepository;
import com.dtthuan3.ecommerce.paymentservice.util.HmacSigner;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Core payment. Nguyen tac doanh nghiep:
 * - Return-url khong bao gio duoc tin; chi IPN (ky hop le) hoac query doi soat moi doi trang thai.
 * - Moi ghi deu idempotent; callback dedup bang event_key unique + audit log day du ke ca chu ky sai.
 * - Doi trang thai luon qua state machine + ghi outbox cung luc (event la hop dong voi order).
 */
@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    public static final String TOPIC = "payment-events";

    private final PaymentRepository payments;
    private final PaymentRefundRepository refunds;
    private final PaymentCallbackLogRepository callbackLogs;
    private final PaymentOutboxRepository outbox;
    private final PaymentProperties props;
    private final ObjectMapper objectMapper;
    private final Map<PaymentMethod, PaymentProvider> providers = new EnumMap<>(PaymentMethod.class);
    private final Map<String, PaymentProvider> providersByCode = new LinkedHashMap<>();

    public PaymentService(PaymentRepository payments, PaymentRefundRepository refunds,
                          PaymentCallbackLogRepository callbackLogs, PaymentOutboxRepository outbox,
                          PaymentProperties props, ObjectMapper objectMapper,
                          List<PaymentProvider> providerList) {
        this.payments = payments;
        this.refunds = refunds;
        this.callbackLogs = callbackLogs;
        this.outbox = outbox;
        this.props = props;
        this.objectMapper = objectMapper;
        for (PaymentProvider p : providerList) {
            providers.put(p.method(), p);
            providersByCode.put(p.providerCode(), p);
        }
    }

    // ---------------------------------------------------------------- create

    public PaymentResponse create(CreatePaymentRequest req, String idempotencyKey, String userId) {
        requireIdempotencyKey(idempotencyKey);
        String hash = HmacSigner.sha256Hex(req.orderId() + "|" + req.method() + "|" + req.amount()
                + "|" + req.currencyOrDefault() + "|" + String.valueOf(req.returnUrl()));

        var existing = payments.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            if (!existing.get().getRequestHash().equals(hash)) {
                throw new BusinessException("IDEMPOTENCY_KEY_REUSED", HttpStatus.CONFLICT,
                        "Idempotency-Key already used with a different payload");
            }
            return PaymentResponse.from(existing.get());
        }
        if (payments.existsByOrderIdAndStatusIn(req.orderId(),
                List.of(PaymentStatus.PENDING, PaymentStatus.PAID, PaymentStatus.PARTIALLY_REFUNDED))) {
            throw new BusinessException("PAYMENT_ALREADY_IN_PROGRESS", HttpStatus.CONFLICT,
                    "Order " + req.orderId() + " already has an active payment");
        }

        Payment p = new Payment();
        p.setId(UUID.randomUUID().toString());
        p.setOrderId(req.orderId());
        p.setUserId(userId);
        p.setIdempotencyKey(idempotencyKey);
        p.setRequestHash(hash);
        p.setMethod(req.method());
        p.setAmount(req.amount());
        p.setCurrency(req.currencyOrDefault());
        if (!"VND".equals(p.getCurrency())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Only VND is supported");
        }
        p.setStatus(PaymentStatus.CREATED);
        p.setProviderTxnRef(req.method().name() + "-" + p.getId().replace("-", ""));
        p.setReturnUrl(req.returnUrl());
        p.setDescription(req.description());
        p.setExpiresAt(req.method().isOnline() ? Instant.now().plus(props.getPendingTtl()) : null);
        payments.save(p);

        try {
            PaymentProvider.ProviderSession session = providers.get(req.method()).createSession(p);
            p.setCheckoutUrl(session.checkoutUrl());
            p.setQrContent(session.qrContent());
            transition(p, PaymentStatus.PENDING);
            payments.save(p);
            emit(p, "PaymentCreated");
            return PaymentResponse.from(p);
        } catch (BusinessException e) {
            // Gateway khong tao duoc session: FAILED la terminal nhung khong chan order retry bang key moi.
            p.setStatus(PaymentStatus.FAILED);
            p.setFailureCode("PROVIDER_UNAVAILABLE");
            p.setFailureMessage(e.getMessage());
            payments.save(p);
            emit(p, "PaymentFailed");
            throw e;
        }
    }

    // ---------------------------------------------------------------- queries

    @Transactional(readOnly = true)
    public PaymentResponse get(String id) {
        return PaymentResponse.from(find(id));
    }

    @Transactional(readOnly = true)
    public PaymentResponse getByOrder(String orderId) {
        return PaymentResponse.from(payments.findFirstByOrderIdOrderByCreatedAtDesc(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found for order " + orderId)));
    }

    // ---------------------------------------------------------------- callback (IPN)

    @Transactional
    public PaymentResponse handleCallback(String providerCode, ProviderCallbackRequest cb) {
        PaymentProvider provider = providersByCode.get(providerCode.toUpperCase());
        if (provider == null) throw new ResourceNotFoundException("Unknown provider " + providerCode);

        String eventKey = HmacSigner.sha256Hex(providerCode + "|" + cb.txnRef() + "|" + cb.resultCode() + "|" + cb.amount());
        String raw = toJson(cb);
        if (callbackLogs.existsByEventKey(eventKey)) {
            log.info("Duplicate callback ignored provider={} txnRef={}", providerCode, cb.txnRef());
            return payments.findByProviderTxnRef(cb.txnRef()).map(PaymentResponse::from)
                    .orElseThrow(() -> new ResourceNotFoundException("Payment not found for txnRef " + cb.txnRef()));
        }

        boolean signatureValid = provider.verifyCallbackSignature(cb.txnRef(), cb.amount(), cb.resultCode(), cb.timestamp(), cb.signature())
                && Math.abs(Instant.now().getEpochSecond() - cb.timestamp()) <= 300;
        if (!signatureValid) {
            saveLog(providerCode, cb, eventKey, raw, false, false);
            throw new BusinessException("SIGNATURE_INVALID", HttpStatus.BAD_REQUEST,
                    "Callback signature is invalid; payment state unchanged");
        }

        Payment p = payments.findByProviderTxnRef(cb.txnRef())
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found for txnRef " + cb.txnRef()));

        if (cb.amount() != p.getAmount()) {
            saveLog(providerCode, cb, eventKey, raw, true, false);
            throw new BusinessException("PAYMENT_AMOUNT_MISMATCH", HttpStatus.UNPROCESSABLE_ENTITY,
                    "Callback amount " + cb.amount() + " != payment amount " + p.getAmount());
        }

        applyResult(p, cb.isSuccess(), cb.resultCode());
        saveLog(providerCode, cb, eventKey, raw, true, true);
        return PaymentResponse.from(p);
    }

    // ---------------------------------------------------------------- sync / reconcile

    public PaymentResponse sync(String id) {
        Payment p = find(id);
        if (!p.getMethod().isOnline() || p.getStatus() == PaymentStatus.PAID || p.getStatus().isTerminal()) {
            return PaymentResponse.from(p);
        }
        PaymentProvider.ProviderStatus st = providers.get(p.getMethod()).query(p.getProviderTxnRef());
        if (st.pending()) return PaymentResponse.from(p);
        if (st.amount() != p.getAmount()) {
            throw new BusinessException("PAYMENT_AMOUNT_MISMATCH", HttpStatus.UNPROCESSABLE_ENTITY,
                    "Gateway amount " + st.amount() + " != payment amount " + p.getAmount());
        }
        applyResult(p, st.success(), st.resultCode());
        payments.save(p);
        return PaymentResponse.from(p);
    }

    // ---------------------------------------------------------------- refund / cancel / COD

    @Transactional
    public RefundResponse refund(String paymentId, CreateRefundRequest req, String idempotencyKey) {
        requireIdempotencyKey(idempotencyKey);
        String hash = HmacSigner.sha256Hex(paymentId + "|" + req.amount() + "|" + String.valueOf(req.reason()));
        var existing = refunds.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            if (!existing.get().getRequestHash().equals(hash)) {
                throw new BusinessException("IDEMPOTENCY_KEY_REUSED", HttpStatus.CONFLICT,
                        "Idempotency-Key already used with a different payload");
            }
            return RefundResponse.from(existing.get());
        }

        Payment p = find(paymentId);
        if (p.getStatus() != PaymentStatus.PAID && p.getStatus() != PaymentStatus.PARTIALLY_REFUNDED) {
            throw new BusinessException(ErrorCode.INVALID_RESOURCE_STATE,
                    "Only PAID/PARTIALLY_REFUNDED payment can be refunded, current: " + p.getStatus());
        }
        long reserved = refunds.sumByPaymentIdAndStatusIn(paymentId, List.of(RefundStatus.PENDING, RefundStatus.SUCCEEDED));
        if (reserved + req.amount() > p.getAmount()) {
            throw new BusinessException("REFUND_EXCEEDS_PAID_AMOUNT", HttpStatus.UNPROCESSABLE_ENTITY,
                    "Refund total would exceed paid amount " + p.getAmount());
        }

        PaymentRefund r = new PaymentRefund();
        r.setId(UUID.randomUUID().toString());
        r.setPaymentId(paymentId);
        r.setIdempotencyKey(idempotencyKey);
        r.setRequestHash(hash);
        r.setAmount(req.amount());
        r.setReason(req.reason());
        r.setStatus(RefundStatus.PENDING);
        refunds.save(r);

        try {
            r.setProviderRefundRef(providers.get(p.getMethod()).refund(p, r));
            r.setStatus(RefundStatus.SUCCEEDED);
        } catch (BusinessException e) {
            r.setStatus(RefundStatus.FAILED);
            refunds.save(r);
            throw e;
        }
        refunds.save(r);

        p.setRefundedAmount(p.getRefundedAmount() + req.amount());
        transition(p, p.getRefundedAmount() >= p.getAmount() ? PaymentStatus.REFUNDED : PaymentStatus.PARTIALLY_REFUNDED);
        payments.save(p);
        emit(p, "PaymentRefunded");
        return RefundResponse.from(r);
    }

    @Transactional
    public PaymentResponse cancel(String id) {
        Payment p = find(id);
        transition(p, PaymentStatus.CANCELLED);
        payments.save(p);
        emit(p, "PaymentCancelled");
        return PaymentResponse.from(p);
    }

    @Transactional
    public PaymentResponse codCollected(String id) {
        Payment p = findCod(id);
        transition(p, PaymentStatus.PAID);
        p.setPaidAt(Instant.now());
        payments.save(p);
        emit(p, "PaymentSucceeded");
        return PaymentResponse.from(p);
    }

    @Transactional
    public PaymentResponse codFailed(String id) {
        Payment p = findCod(id);
        transition(p, PaymentStatus.FAILED);
        p.setFailureCode("COD_NOT_COLLECTED");
        p.setFailureMessage("Delivery failed or goods returned; COD not collected");
        payments.save(p);
        emit(p, "PaymentFailed");
        return PaymentResponse.from(p);
    }

    // ---------------------------------------------------------------- jobs (goi tu scheduler)

    @Transactional
    public int expireOverdue() {
        List<Payment> overdue = payments.findByStatusAndExpiresAtBefore(PaymentStatus.PENDING, Instant.now());
        int count = 0;
        for (Payment p : overdue) {
            if (p.getMethod() == PaymentMethod.COD) continue;
            transition(p, PaymentStatus.EXPIRED);
            payments.save(p);
            emit(p, "PaymentExpired");
            count++;
        }
        return count;
    }

    public int reconcileStale() {
        Instant cutoff = Instant.now().minus(props.getReconcileAfter());
        int count = 0;
        for (Payment p : payments.findByStatusAndUpdatedAtBefore(PaymentStatus.PENDING, cutoff)) {
            if (!p.getMethod().isOnline()) continue;
            try {
                sync(p.getId());
                count++;
            } catch (Exception e) {
                log.warn("Reconcile failed payment={}: {}", p.getId(), e.getMessage());
            }
        }
        return count;
    }

    // ---------------------------------------------------------------- helpers

    private void applyResult(Payment p, boolean success, String resultCode) {
        if (success) {
            if (p.getStatus() == PaymentStatus.PAID) return; // IPN den sau khi da PAID: no-op
            boolean late = p.getStatus() == PaymentStatus.EXPIRED
                    || (p.getExpiresAt() != null && Instant.now().isAfter(p.getExpiresAt()));
            transition(p, PaymentStatus.PAID);
            p.setPaidAt(Instant.now());
            p.setLatePayment(late);
            payments.save(p);
            emit(p, "PaymentSucceeded");
        } else {
            if (p.getStatus() == PaymentStatus.PENDING || p.getStatus() == PaymentStatus.CREATED) {
                transition(p, PaymentStatus.FAILED);
                p.setFailureCode(resultCode);
                p.setFailureMessage("Provider reported failure, resultCode=" + resultCode);
                payments.save(p);
                emit(p, "PaymentFailed");
            }
        }
    }

    private void transition(Payment p, PaymentStatus target) {
        if (p.getStatus() == target) return;
        if (!p.getStatus().canTransitionTo(target)) {
            throw new BusinessException(ErrorCode.INVALID_RESOURCE_STATE,
                    "Payment " + p.getId() + " cannot transition " + p.getStatus() + " -> " + target);
        }
        p.setStatus(target);
    }

    private Payment find(String id) {
        return payments.findById(id).orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + id));
    }

    private Payment findCod(String id) {
        Payment p = find(id);
        if (p.getMethod() != PaymentMethod.COD) {
            throw new BusinessException(ErrorCode.INVALID_OPERATION, "Payment " + id + " is not COD");
        }
        return p;
    }

    private void saveLog(String provider, ProviderCallbackRequest cb, String eventKey, String raw,
                         boolean signatureValid, boolean processed) {
        PaymentCallbackLog entry = new PaymentCallbackLog();
        entry.setId(UUID.randomUUID().toString());
        entry.setProvider(provider);
        entry.setProviderTxnRef(cb.txnRef());
        entry.setEventKey(eventKey);
        entry.setSignatureValid(signatureValid);
        entry.setProcessed(processed);
        entry.setRawPayload(raw);
        callbackLogs.save(entry);
    }

    private void emit(Payment p, String eventType) {
        String eventId = UUID.randomUUID().toString();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("eventId", eventId);
        payload.put("eventType", eventType);
        payload.put("paymentId", p.getId());
        payload.put("orderId", p.getOrderId());
        payload.put("method", p.getMethod().name());
        payload.put("amount", p.getAmount());
        payload.put("currency", p.getCurrency());
        payload.put("status", p.getStatus().name());
        payload.put("refundedAmount", p.getRefundedAmount());
        payload.put("latePayment", p.isLatePayment());
        payload.put("occurredAt", Instant.now().toString());

        PaymentOutbox o = new PaymentOutbox();
        o.setEventId(eventId);
        o.setAggregateId(p.getId());
        o.setEventType(eventType);
        o.setTopic(TOPIC);
        o.setPayload(toJson(payload));
        outbox.save(o);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialize payload", e);
        }
    }

    private void requireIdempotencyKey(String key) {
        if (key == null || key.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Idempotency-Key header is required");
        }
    }
}
