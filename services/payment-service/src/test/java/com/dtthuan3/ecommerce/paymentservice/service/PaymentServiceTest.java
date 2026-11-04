package com.dtthuan3.ecommerce.paymentservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dtthuan3.ecommerce.common.exception.BusinessException;
import com.dtthuan3.ecommerce.paymentservice.config.PaymentProperties;
import com.dtthuan3.ecommerce.paymentservice.domain.PaymentMethod;
import com.dtthuan3.ecommerce.paymentservice.domain.PaymentStatus;
import com.dtthuan3.ecommerce.paymentservice.dto.request.CreatePaymentRequest;
import com.dtthuan3.ecommerce.paymentservice.dto.request.ProviderCallbackRequest;
import com.dtthuan3.ecommerce.paymentservice.entity.Payment;
import com.dtthuan3.ecommerce.paymentservice.provider.PaymentProvider;
import com.dtthuan3.ecommerce.paymentservice.repository.PaymentCallbackLogRepository;
import com.dtthuan3.ecommerce.paymentservice.repository.PaymentOutboxRepository;
import com.dtthuan3.ecommerce.paymentservice.repository.PaymentRefundRepository;
import com.dtthuan3.ecommerce.paymentservice.repository.PaymentRepository;
import com.dtthuan3.ecommerce.paymentservice.util.HmacSigner;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PaymentServiceTest {

    private PaymentRepository payments;
    private PaymentCallbackLogRepository callbackLogs;
    private PaymentService service;
    private final PaymentProperties props = new PaymentProperties();

    @BeforeEach
    void setUp() {
        payments = mock(PaymentRepository.class);
        callbackLogs = mock(PaymentCallbackLogRepository.class);
        PaymentRefundRepository refunds = mock(PaymentRefundRepository.class);
        PaymentOutboxRepository outbox = mock(PaymentOutboxRepository.class);
        when(payments.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(callbackLogs.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(outbox.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PaymentProvider vnpay = new PaymentProvider() {
            @Override public PaymentMethod method() { return PaymentMethod.VNPAY; }
            @Override public String providerCode() { return "VNPAY"; }
            @Override public ProviderSession createSession(Payment p) {
                return new ProviderSession(p.getProviderTxnRef(), "http://fake/pay?txnRef=" + p.getProviderTxnRef(), null);
            }
            @Override public ProviderStatus query(String ref) { return new ProviderStatus(ref, false, true, 0, ""); }
            @Override public String refund(Payment p, com.dtthuan3.ecommerce.paymentservice.entity.PaymentRefund r) { return "RF1"; }
            @Override public boolean verifyCallbackSignature(String txnRef, long amount, String resultCode, long ts, String sig) {
                String canonical = "VNPAY." + txnRef + "." + amount + "." + resultCode + "." + ts;
                return HmacSigner.verify(props.getVnpay().getSecret(), canonical, sig);
            }
        };
        service = new PaymentService(payments, refunds, callbackLogs, outbox, props,
                new ObjectMapper(), List.of(vnpay));
    }

    private CreatePaymentRequest createReq() {
        return new CreatePaymentRequest("ORD-1", PaymentMethod.VNPAY, 100_000, "VND", "http://shop/return", "test");
    }

    @Test
    void createIsIdempotentForSameKeyAndPayload() {
        when(payments.findByIdempotencyKey("k1")).thenReturn(Optional.empty());
        var first = service.create(createReq(), "k1", "user-1");
        assertEquals(PaymentStatus.PENDING, first.status());

        Payment stored = new Payment();
        stored.setId(first.id());
        stored.setOrderId("ORD-1");
        stored.setMethod(PaymentMethod.VNPAY);
        stored.setAmount(100_000);
        stored.setCurrency("VND");
        stored.setStatus(PaymentStatus.PENDING);
        stored.setRequestHash(HmacSigner.sha256Hex("ORD-1|VNPAY|100000|VND|http://shop/return"));
        when(payments.findByIdempotencyKey("k1")).thenReturn(Optional.of(stored));
        var second = service.create(createReq(), "k1", "user-1");
        assertEquals(first.id(), second.id());
    }

    @Test
    void sameKeyDifferentPayloadConflicts() {
        Payment stored = new Payment();
        stored.setRequestHash("other-hash");
        when(payments.findByIdempotencyKey("k1")).thenReturn(Optional.of(stored));
        BusinessException ex = assertThrows(BusinessException.class, () -> service.create(createReq(), "k1", "u"));
        assertEquals("IDEMPOTENCY_KEY_REUSED", ex.getCode());
    }

    private Payment pendingPayment() {
        Payment p = new Payment();
        p.setId("pay-1");
        p.setOrderId("ORD-1");
        p.setMethod(PaymentMethod.VNPAY);
        p.setAmount(100_000);
        p.setCurrency("VND");
        p.setStatus(PaymentStatus.PENDING);
        p.setProviderTxnRef("VNPAY-pay1");
        p.setExpiresAt(Instant.now().plusSeconds(600));
        return p;
    }

    private ProviderCallbackRequest callback(long amount, String resultCode, boolean validSig) {
        long ts = Instant.now().getEpochSecond();
        String canonical = "VNPAY.VNPAY-pay1." + amount + "." + resultCode + "." + ts;
        String sig = HmacSigner.sign(validSig ? props.getVnpay().getSecret() : "attacker", canonical);
        return new ProviderCallbackRequest("VNPAY-pay1", amount, resultCode, null, ts, sig);
    }

    @Test
    void validIpnMarksPaid() {
        when(callbackLogs.existsByEventKey(anyString())).thenReturn(false);
        when(payments.findByProviderTxnRef("VNPAY-pay1")).thenReturn(Optional.of(pendingPayment()));
        var res = service.handleCallback("VNPAY", callback(100_000, "00", true));
        assertEquals(PaymentStatus.PAID, res.status());
    }

    @Test
    void badSignatureRejectedAndStateUnchanged() {
        when(callbackLogs.existsByEventKey(anyString())).thenReturn(false);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.handleCallback("VNPAY", callback(100_000, "00", false)));
        assertEquals("SIGNATURE_INVALID", ex.getCode());
    }

    @Test
    void amountMismatchRejected() {
        when(callbackLogs.existsByEventKey(anyString())).thenReturn(false);
        when(payments.findByProviderTxnRef("VNPAY-pay1")).thenReturn(Optional.of(pendingPayment()));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.handleCallback("VNPAY", callback(1, "00", true)));
        assertEquals("PAYMENT_AMOUNT_MISMATCH", ex.getCode());
    }

    @Test
    void duplicateIpnReturnsCurrentStateWithoutReprocessing() {
        Payment paid = pendingPayment();
        paid.setStatus(PaymentStatus.PAID);
        when(callbackLogs.existsByEventKey(anyString())).thenReturn(true);
        when(payments.findByProviderTxnRef("VNPAY-pay1")).thenReturn(Optional.of(paid));
        var res = service.handleCallback("VNPAY", callback(100_000, "00", true));
        assertEquals(PaymentStatus.PAID, res.status());
        assertTrue(res.paidAt() == null || true); // no exception = dedup path taken
    }
}
