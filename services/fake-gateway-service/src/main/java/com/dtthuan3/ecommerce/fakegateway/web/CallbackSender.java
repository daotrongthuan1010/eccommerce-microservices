package com.dtthuan3.ecommerce.fakegateway.web;

import com.dtthuan3.ecommerce.fakegateway.config.FakeProperties;
import com.dtthuan3.ecommerce.fakegateway.store.FakeStore;
import com.dtthuan3.ecommerce.fakegateway.util.FakeHmac;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Ban IPN/callback ve payment-service theo dung protocol da thong nhat:
 * canonical = provider.txnRef.amount.resultCode.timestamp, ky HMAC-SHA256.
 * Kich ban loi (chi de test): DROP_IPN (khong ban), DUPLICATE_IPN (ban 2 lan),
 * BAD_SIGNATURE (ky sai), AMOUNT_TAMPER (amount +1000).
 */
@Component
public class CallbackSender {

    private static final Logger log = LoggerFactory.getLogger(CallbackSender.class);
    private final FakeProperties props;
    private final RestClient restClient = RestClient.create();

    public CallbackSender(FakeProperties props) {
        this.props = props;
    }

    public void sendPaymentCallback(FakeStore.FakePaymentOrder order) {
        if (order.ipnUrl == null || order.ipnUrl.isBlank()) return;
        if ("DROP_IPN".equalsIgnoreCase(order.scenario)) {
            log.info("[FAKE] DROP_IPN: khong ban callback cho txnRef={}", order.txnRef);
            return;
        }
        sendOnce(order);
        if ("DUPLICATE_IPN".equalsIgnoreCase(order.scenario)) {
            log.info("[FAKE] DUPLICATE_IPN: ban lan 2 cho txnRef={}", order.txnRef);
            sendOnce(order);
        }
    }

    private void sendOnce(FakeStore.FakePaymentOrder order) {
        long ts = Instant.now().getEpochSecond();
        long amount = "AMOUNT_TAMPER".equalsIgnoreCase(order.scenario) ? order.amount + 1000 : order.amount;
        String secret = "VNPAY".equals(order.provider) ? props.getVnpaySecret() : props.getZalopaySecret();
        if ("BAD_SIGNATURE".equalsIgnoreCase(order.scenario)) secret = "attacker-secret";
        String canonical = order.provider + "." + order.txnRef + "." + amount + "." + order.resultCode + "." + ts;

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("txnRef", order.txnRef);
        body.put("amount", amount);
        body.put("resultCode", order.resultCode);
        body.put("status", order.status);
        body.put("timestamp", ts);
        body.put("signature", FakeHmac.sign(secret, canonical));
        try {
            restClient.post().uri(order.ipnUrl).body(body).retrieve().toBodilessEntity();
            log.info("[FAKE] Da ban callback {} txnRef={} status={}", order.provider, order.txnRef, order.status);
        } catch (Exception e) {
            log.warn("[FAKE] Ban callback that bai txnRef={}: {}", order.txnRef, e.getMessage());
        }
    }

    /** Ban webhook trang thai van don ve shipping-service. Canonical: carrierCode.status.timestamp */
    public void sendCarrierWebhook(FakeStore.FakeCarrierShipment shipment, String note) {
        if (shipment.webhookUrl == null || shipment.webhookUrl.isBlank()) return;
        long ts = Instant.now().getEpochSecond();
        String canonical = shipment.carrierCode + "." + shipment.status + "." + ts;
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("carrierCode", shipment.carrierCode);
        body.put("status", shipment.status);
        body.put("timestamp", ts);
        body.put("signature", FakeHmac.sign(props.getCarrierSecret(), canonical));
        body.put("note", note == null ? "" : note);
        try {
            restClient.post().uri(shipment.webhookUrl).body(body).retrieve().toBodilessEntity();
            log.info("[FAKE] Da ban carrier webhook {} status={}", shipment.carrierCode, shipment.status);
        } catch (Exception e) {
            log.warn("[FAKE] Ban carrier webhook that bai {}: {}", shipment.carrierCode, e.getMessage());
        }
    }
}
