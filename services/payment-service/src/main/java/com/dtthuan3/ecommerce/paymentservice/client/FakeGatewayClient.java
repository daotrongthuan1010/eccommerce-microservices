package com.dtthuan3.ecommerce.paymentservice.client;

import com.dtthuan3.ecommerce.common.exception.BusinessException;
import com.dtthuan3.ecommerce.common.exception.ErrorCode;
import com.dtthuan3.ecommerce.paymentservice.util.HmacSigner;
import java.time.Instant;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Client goi fake-gateway-service (process rieng, secret rieng — dung trust-boundary nhu cong that).
 * Moi request ky HMAC-SHA256 tren canonical "txnRef.amount.timestamp" + header X-Timestamp chong replay.
 */
@Component
public class FakeGatewayClient {

    private static final Logger log = LoggerFactory.getLogger(FakeGatewayClient.class);
    private final RestClient restClient;

    public FakeGatewayClient(RestClient fakeGatewayRestClient) {
        this.restClient = fakeGatewayRestClient;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> createOrder(String providerPath, String secret, String scenario,
                                           String txnRef, long amount, String returnUrl, String ipnUrl) {
        long ts = Instant.now().getEpochSecond();
        try {
            Map<String, Object> body = restClient.post()
                    .uri("/fake/{provider}/orders", providerPath)
                    .header("X-Timestamp", String.valueOf(ts))
                    .header("X-Signature", HmacSigner.sign(secret, txnRef + "." + amount + "." + ts))
                    .header("X-Fake-Scenario", scenario == null ? "" : scenario)
                    .body(Map.of("txnRef", txnRef, "amount", amount,
                            "returnUrl", returnUrl == null ? "" : returnUrl,
                            "ipnUrl", ipnUrl == null ? "" : ipnUrl))
                    .retrieve()
                    .body(Map.class);
            if (body == null) throw providerDown("Empty response from fake gateway");
            return body;
        } catch (RestClientException e) {
            log.warn("Fake gateway createOrder failed provider={} txnRef={}: {}", providerPath, txnRef, e.getMessage());
            throw providerDown("Fake gateway unavailable: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> queryOrder(String providerPath, String secret, String txnRef) {
        long ts = Instant.now().getEpochSecond();
        try {
            Map<String, Object> body = restClient.get()
                    .uri("/fake/{provider}/orders/{txnRef}", providerPath, txnRef)
                    .header("X-Timestamp", String.valueOf(ts))
                    .header("X-Signature", HmacSigner.sign(secret, txnRef + ".0." + ts))
                    .retrieve()
                    .body(Map.class);
            if (body == null) throw providerDown("Empty query response from fake gateway");
            return body;
        } catch (RestClientException e) {
            log.warn("Fake gateway query failed provider={} txnRef={}: {}", providerPath, txnRef, e.getMessage());
            throw providerDown("Fake gateway unavailable: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> refund(String providerPath, String secret, String txnRef, long amount, String refundRef) {
        long ts = Instant.now().getEpochSecond();
        try {
            Map<String, Object> body = restClient.post()
                    .uri("/fake/{provider}/refunds", providerPath)
                    .header("X-Timestamp", String.valueOf(ts))
                    .header("X-Signature", HmacSigner.sign(secret, txnRef + "." + amount + "." + ts))
                    .body(Map.of("txnRef", txnRef, "amount", amount, "refundRef", refundRef))
                    .retrieve()
                    .body(Map.class);
            if (body == null) throw providerDown("Empty refund response from fake gateway");
            return body;
        } catch (RestClientException e) {
            log.warn("Fake gateway refund failed provider={} txnRef={}: {}", providerPath, txnRef, e.getMessage());
            throw providerDown("Fake gateway unavailable: " + e.getMessage());
        }
    }

    private BusinessException providerDown(String message) {
        return new BusinessException(ErrorCode.DEPENDENCY_ERROR, message);
    }
}
