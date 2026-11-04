package com.dtthuan3.ecommerce.paymentservice.provider;

import com.dtthuan3.ecommerce.paymentservice.client.FakeGatewayClient;
import com.dtthuan3.ecommerce.paymentservice.config.PaymentProperties;
import com.dtthuan3.ecommerce.paymentservice.domain.PaymentMethod;
import com.dtthuan3.ecommerce.paymentservice.entity.Payment;
import com.dtthuan3.ecommerce.paymentservice.entity.PaymentRefund;
import com.dtthuan3.ecommerce.paymentservice.util.HmacSigner;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Fake VNPay: redirect hosted-page + IPN. Flow chuan VNPay de sau nay thay bang adapter that. */
@Component
public class FakeVnPayProvider implements PaymentProvider {

    private final FakeGatewayClient gateway;
    private final PaymentProperties props;

    public FakeVnPayProvider(FakeGatewayClient gateway, PaymentProperties props) {
        this.gateway = gateway;
        this.props = props;
    }

    @Override public PaymentMethod method() { return PaymentMethod.VNPAY; }
    @Override public String providerCode() { return "VNPAY"; }

    @Override
    public ProviderSession createSession(Payment payment) {
        String ipnUrl = props.getPublicBaseUrl() + "/payments/webhooks/VNPAY";
        Map<String, Object> res = gateway.createOrder("vnpay", props.getVnpay().getSecret(),
                props.getVnpay().getScenario(), payment.getProviderTxnRef(), payment.getAmount(),
                payment.getReturnUrl(), ipnUrl);
        return new ProviderSession(payment.getProviderTxnRef(), (String) res.get("payUrl"), null);
    }

    @Override
    public ProviderStatus query(String providerTxnRef) {
        Map<String, Object> res = gateway.queryOrder("vnpay", props.getVnpay().getSecret(), providerTxnRef);
        return toStatus(providerTxnRef, res);
    }

    @Override
    public String refund(Payment payment, PaymentRefund refund) {
        Map<String, Object> res = gateway.refund("vnpay", props.getVnpay().getSecret(),
                payment.getProviderTxnRef(), refund.getAmount(), refund.getId());
        return (String) res.getOrDefault("refundRef", refund.getId());
    }

    @Override
    public boolean verifyCallbackSignature(String txnRef, long amount, String resultCode, long timestamp, String signature) {
        String canonical = providerCode() + "." + txnRef + "." + amount + "." + resultCode + "." + timestamp;
        return HmacSigner.verify(props.getVnpay().getSecret(), canonical, signature);
    }

    static ProviderStatus toStatus(String txnRef, Map<String, Object> res) {
        String status = (String) res.getOrDefault("status", "PENDING");
        long amount = res.get("amount") instanceof Number n ? n.longValue() : 0L;
        String resultCode = (String) res.getOrDefault("resultCode", "");
        return new ProviderStatus(txnRef, "SUCCESS".equals(status), "PENDING".equals(status), amount, resultCode);
    }
}
