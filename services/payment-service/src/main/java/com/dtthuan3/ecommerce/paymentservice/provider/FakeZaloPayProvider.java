package com.dtthuan3.ecommerce.paymentservice.provider;

import com.dtthuan3.ecommerce.paymentservice.client.FakeGatewayClient;
import com.dtthuan3.ecommerce.paymentservice.config.PaymentProperties;
import com.dtthuan3.ecommerce.paymentservice.domain.PaymentMethod;
import com.dtthuan3.ecommerce.paymentservice.entity.Payment;
import com.dtthuan3.ecommerce.paymentservice.entity.PaymentRefund;
import com.dtthuan3.ecommerce.paymentservice.util.HmacSigner;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Fake ZaloPay: khong co hosted-page — tra qrContent/orderUrl, ket qua chi ve bang
 * callback server-to-server. Khac flow VNPay co chu dich de chung minh abstraction.
 */
@Component
public class FakeZaloPayProvider implements PaymentProvider {

    private final FakeGatewayClient gateway;
    private final PaymentProperties props;

    public FakeZaloPayProvider(FakeGatewayClient gateway, PaymentProperties props) {
        this.gateway = gateway;
        this.props = props;
    }

    @Override public PaymentMethod method() { return PaymentMethod.ZALOPAY; }
    @Override public String providerCode() { return "ZALOPAY"; }

    @Override
    public ProviderSession createSession(Payment payment) {
        String callbackUrl = props.getPublicBaseUrl() + "/payments/webhooks/ZALOPAY";
        Map<String, Object> res = gateway.createOrder("zalopay", props.getZalopay().getSecret(),
                props.getZalopay().getScenario(), payment.getProviderTxnRef(), payment.getAmount(),
                payment.getReturnUrl(), callbackUrl);
        return new ProviderSession(payment.getProviderTxnRef(),
                (String) res.get("orderUrl"), (String) res.get("qrContent"));
    }

    @Override
    public ProviderStatus query(String providerTxnRef) {
        Map<String, Object> res = gateway.queryOrder("zalopay", props.getZalopay().getSecret(), providerTxnRef);
        return FakeVnPayProvider.toStatus(providerTxnRef, res);
    }

    @Override
    public String refund(Payment payment, PaymentRefund refund) {
        Map<String, Object> res = gateway.refund("zalopay", props.getZalopay().getSecret(),
                payment.getProviderTxnRef(), refund.getAmount(), refund.getId());
        return (String) res.getOrDefault("refundRef", refund.getId());
    }

    @Override
    public boolean verifyCallbackSignature(String txnRef, long amount, String resultCode, long timestamp, String signature) {
        String canonical = providerCode() + "." + txnRef + "." + amount + "." + resultCode + "." + timestamp;
        return HmacSigner.verify(props.getZalopay().getSecret(), canonical, signature);
    }
}
