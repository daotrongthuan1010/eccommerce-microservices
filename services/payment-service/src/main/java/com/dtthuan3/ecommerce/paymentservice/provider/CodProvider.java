package com.dtthuan3.ecommerce.paymentservice.provider;

import com.dtthuan3.ecommerce.common.exception.BusinessException;
import com.dtthuan3.ecommerce.common.exception.ErrorCode;
import com.dtthuan3.ecommerce.paymentservice.domain.PaymentMethod;
import com.dtthuan3.ecommerce.paymentservice.entity.Payment;
import com.dtthuan3.ecommerce.paymentservice.entity.PaymentRefund;
import org.springframework.stereotype.Component;

/** COD: khong qua gateway. Trang thai PAID/FAILED do order bao qua /cod/collected | /cod/failed. */
@Component
public class CodProvider implements PaymentProvider {

    @Override public PaymentMethod method() { return PaymentMethod.COD; }
    @Override public String providerCode() { return "COD"; }

    @Override
    public ProviderSession createSession(Payment payment) {
        return new ProviderSession(payment.getProviderTxnRef(), null, null);
    }

    @Override
    public ProviderStatus query(String providerTxnRef) {
        throw new BusinessException(ErrorCode.INVALID_OPERATION, "COD payment has no gateway to query");
    }

    @Override
    public String refund(Payment payment, PaymentRefund refund) {
        throw new BusinessException(ErrorCode.INVALID_OPERATION, "COD payment cannot be refunded online");
    }

    @Override
    public boolean verifyCallbackSignature(String txnRef, long amount, String resultCode, long timestamp, String signature) {
        return false;
    }
}
