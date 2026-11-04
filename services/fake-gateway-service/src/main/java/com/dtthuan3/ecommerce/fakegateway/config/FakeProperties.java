package com.dtthuan3.ecommerce.fakegateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Secret phai trung voi cau hinh ben payment-service / shipping-service. */
@Component
@ConfigurationProperties(prefix = "fake")
public class FakeProperties {
    private String publicBaseUrl = "http://localhost:8099";
    private String vnpaySecret = "local_fake_vnpay_change_me";
    private String zalopaySecret = "local_fake_zalopay_change_me";
    private String carrierSecret = "local_fake_carrier_change_me";

    public String getPublicBaseUrl() { return publicBaseUrl; }
    public void setPublicBaseUrl(String publicBaseUrl) { this.publicBaseUrl = publicBaseUrl; }
    public String getVnpaySecret() { return vnpaySecret; }
    public void setVnpaySecret(String vnpaySecret) { this.vnpaySecret = vnpaySecret; }
    public String getZalopaySecret() { return zalopaySecret; }
    public void setZalopaySecret(String zalopaySecret) { this.zalopaySecret = zalopaySecret; }
    public String getCarrierSecret() { return carrierSecret; }
    public void setCarrierSecret(String carrierSecret) { this.carrierSecret = carrierSecret; }
}
