package com.dtthuan3.ecommerce.shippingservice.provider;

import com.dtthuan3.ecommerce.common.exception.BusinessException;
import com.dtthuan3.ecommerce.common.exception.ErrorCode;
import com.dtthuan3.ecommerce.shippingservice.config.ShippingProperties;
import com.dtthuan3.ecommerce.shippingservice.entity.Shipment;
import com.dtthuan3.ecommerce.shippingservice.util.HmacSigner;
import java.time.Instant;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Adapter goi fake carrier trong fake-gateway-service (ky HMAC, timeout chat). */
@Component
public class FakeGhnCarrierProvider implements CarrierProvider {

    private static final Logger log = LoggerFactory.getLogger(FakeGhnCarrierProvider.class);
    private final RestClient restClient;
    private final ShippingProperties props;

    public FakeGhnCarrierProvider(RestClient fakeCarrierRestClient, ShippingProperties props) {
        this.restClient = fakeCarrierRestClient;
        this.props = props;
    }

    @Override public String carrierCode() { return "FAKE_GHN"; }

    @Override
    @SuppressWarnings("unchecked")
    public String createShipment(Shipment s) {
        long ts = Instant.now().getEpochSecond();
        String signature = HmacSigner.sign(props.getFakeGhn().getSecret(),
                s.getId() + "." + s.getCodAmount() + "." + ts);
        try {
            Map<String, Object> res = restClient.post()
                    .uri("/fake/carrier/shipments")
                    .header("X-Timestamp", String.valueOf(ts))
                    .header("X-Signature", signature)
                    .body(Map.of(
                            "clientShipmentId", s.getId(),
                            "orderId", s.getOrderId(),
                            "codAmount", s.getCodAmount(),
                            "shippingFee", s.getShippingFee(),
                            "recipientName", s.getRecipientName(),
                            "recipientPhone", s.getRecipientPhone(),
                            "recipientAddress", s.getRecipientAddress(),
                            "recipientProvinceCode", s.getRecipientProvinceCode(),
                            "webhookUrl", props.getPublicBaseUrl() + "/shipments/webhooks/carrier"))
                    .retrieve()
                    .body(Map.class);
            if (res == null || res.get("carrierCode") == null) {
                throw new BusinessException(ErrorCode.DEPENDENCY_ERROR, "Fake carrier returned no carrierCode");
            }
            return (String) res.get("carrierCode");
        } catch (RestClientException e) {
            log.warn("Fake carrier createShipment failed shipment={}: {}", s.getId(), e.getMessage());
            throw new BusinessException(ErrorCode.DEPENDENCY_ERROR, "Fake carrier unavailable: " + e.getMessage());
        }
    }

    @Override
    public void cancelShipment(String carrierCode) {
        long ts = Instant.now().getEpochSecond();
        String signature = HmacSigner.sign(props.getFakeGhn().getSecret(), carrierCode + ".CANCELLED." + ts);
        try {
            restClient.post()
                    .uri("/fake/carrier/shipments/{code}/cancel", carrierCode)
                    .header("X-Timestamp", String.valueOf(ts))
                    .header("X-Signature", signature)
                    .body(Map.of())
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            // Huy la best-effort: trang thai noi bo da CANCELLED, carrier se duoc doi soat sau.
            log.warn("Fake carrier cancel failed carrierCode={}: {}", carrierCode, e.getMessage());
        }
    }

    @Override
    public boolean verifyWebhookSignature(String carrierCode, String status, long timestamp, String signature) {
        return HmacSigner.verify(props.getFakeGhn().getSecret(), carrierCode + "." + status + "." + timestamp, signature);
    }
}
