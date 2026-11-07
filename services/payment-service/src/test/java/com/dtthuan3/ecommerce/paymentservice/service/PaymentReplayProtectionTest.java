package com.dtthuan3.ecommerce.paymentservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dtthuan3.ecommerce.common.exception.BusinessException;
import com.dtthuan3.ecommerce.paymentservice.config.PaymentProperties;
import com.dtthuan3.ecommerce.paymentservice.domain.PaymentMethod;
import com.dtthuan3.ecommerce.paymentservice.domain.PaymentStatus;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * PAY-UC06 (P0 security, gap tu TEST_COVERAGE_REPORT.md): replay protection —
 * callback co chu ky HOP LE nhung timestamp qua cua (window 300s) phai bi tu choi.
 * Ke tan cong bat callback cu tren mang roi gui lai khong duoc lam payment
 * chuyen trang thai 2 lan. Truoc day chi test chu ky sai, chua test replay.
 */
class PaymentReplayProtectionTest {

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
        props.getVnpay().setSecret("test-secret");

        PaymentProvider vnpay = new PaymentProvider() {
            @Override public PaymentMethod method() { return PaymentMethod.VNPAY; }
            @Override public String providerCode() { return "VNPAY"; }
            @Override public ProviderSession createSession(Payment p) {
                return new ProviderSession(p.getProviderTxnRef(), "http://fake/pay", null);
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

    private Payment pendingPayment() {
        Payment p = new Payment();
        p.setId("pay1");
        p.setOrderId("ORD-1");
        p.setMethod(PaymentMethod.VNPAY);
        p.setAmount(100_000);
        p.setCurrency("VND");
        p.setStatus(PaymentStatus.PENDING);
        p.setProviderTxnRef("VNPAY-pay1");
        p.setExpiresAt(Instant.now().plusSeconds(600));
        return p;
    }

    /** Callback chu ky hop le nhung timestamp 10 phut truoc — ngoai window 300s. */
    private ProviderCallbackRequest replayedCallback() {
        long oldTs = Instant.now().getEpochSecond() - 600;
        String canonical = "VNPAY.VNPAY-pay1.100000.00." + oldTs;
        String sig = HmacSigner.sign(props.getVnpay().getSecret(), canonical);
        return new ProviderCallbackRequest("VNPAY-pay1", 100_000, "00", null, oldTs, sig);
    }

    @Test
    @DisplayName("PAY-UC06: callback chu ky dung nhung timestamp cu 10 phut -> SIGNATURE_INVALID, trang thai khong doi")
    void replayedCallback_rejected() {
        when(callbackLogs.existsByEventKey(anyString())).thenReturn(false);
        when(payments.findByProviderTxnRef("VNPAY-pay1")).thenReturn(Optional.of(pendingPayment()));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.handleCallback("VNPAY", replayedCallback()));

        assertEquals("SIGNATURE_INVALID", ex.getCode(),
                "Replay (timestamp > 300s window) phai bi tu choi giong chu ky sai");
    }

    @Test
    @DisplayName("PAY-UC07: callback vua xay ra (trong window) -> chap nhan, PAID binh thuong")
    void freshCallback_accepted() {
        when(callbackLogs.existsByEventKey(anyString())).thenReturn(false);
        when(payments.findByProviderTxnRef("VNPAY-pay1")).thenReturn(Optional.of(pendingPayment()));

        long ts = Instant.now().getEpochSecond();
        String canonical = "VNPAY.VNPAY-pay1.100000.00." + ts;
        String sig = HmacSigner.sign(props.getVnpay().getSecret(), canonical);
        var cb = new ProviderCallbackRequest("VNPAY-pay1", 100_000, "00", null, ts, sig);

        var res = service.handleCallback("VNPAY", cb);

        assertEquals(PaymentStatus.PAID, res.status());
    }
}
