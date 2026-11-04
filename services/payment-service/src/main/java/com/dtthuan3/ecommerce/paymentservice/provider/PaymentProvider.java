package com.dtthuan3.ecommerce.paymentservice.provider;

import com.dtthuan3.ecommerce.paymentservice.domain.PaymentMethod;
import com.dtthuan3.ecommerce.paymentservice.entity.Payment;
import com.dtthuan3.ecommerce.paymentservice.entity.PaymentRefund;

/**
 * Abstraction cho cong thanh toan. Hom nay la fake VNPay/ZaloPay/COD;
 * co merchant that chi can them adapter moi, core payment va order khong doi.
 */
public interface PaymentProvider {

    PaymentMethod method();

    /** Ma provider dung trong webhook path + callback log: VNPAY | ZALOPAY | COD. */
    String providerCode();

    ProviderSession createSession(Payment payment);

    /** Doi soat chu dong khi mat IPN (query API cua cong). */
    ProviderStatus query(String providerTxnRef);

    /** Tra ve ma refund ben cong. COD khong ho tro (service chan truoc). */
    String refund(Payment payment, PaymentRefund refund);

    /** Kiem chu ky callback: canonical = provider.txnRef.amount.resultCode.timestamp */
    boolean verifyCallbackSignature(String txnRef, long amount, String resultCode, long timestamp, String signature);

    record ProviderSession(String providerTxnRef, String checkoutUrl, String qrContent) {}

    /** success/pending phan biet ro: pending = cong chua co ket qua cuoi. */
    record ProviderStatus(String providerTxnRef, boolean success, boolean pending, long amount, String resultCode) {}
}
