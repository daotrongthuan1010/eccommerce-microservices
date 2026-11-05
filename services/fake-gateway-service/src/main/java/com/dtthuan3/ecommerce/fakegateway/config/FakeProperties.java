package com.dtthuan3.ecommerce.fakegateway.config;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Secret phai trung voi cau hinh ben payment-service / shipping-service.
 *
 * publicBaseUrl phai la URL TUYET DOI public (https://...) vi payUrl sinh ra
 * cho TRINH DUYET — URL noi bo (http://ten-service:port) lam khach khong the
 * mo trang thanh toan (bug E2E 2026-11-05). Kiem tra ngay khi khoi dong de
 * fail-fast thay vi phat hien o buoc thanh toan.
 */
@Component
@ConfigurationProperties(prefix = "fake")
public class FakeProperties {
    private String publicBaseUrl = "http://localhost:8099";
    private String vnpaySecret = "local_fake_vnpay_change_me";
    private String zalopaySecret = "local_fake_zalopay_change_me";
    private String carrierSecret = "local_fake_carrier_change_me";

    @PostConstruct
    void validate() {
        if (publicBaseUrl == null || !(publicBaseUrl.startsWith("http://") || publicBaseUrl.startsWith("https://"))) {
            throw new IllegalStateException(
                    "fake.public-base-url phai la URL tuyet doi (http/https): " + publicBaseUrl);
        }
    }

    public String getPublicBaseUrl() { return publicBaseUrl; }
    public void setPublicBaseUrl(String publicBaseUrl) { this.publicBaseUrl = publicBaseUrl; }
    public String getVnpaySecret() { return vnpaySecret; }
    public void setVnpaySecret(String vnpaySecret) { this.vnpaySecret = vnpaySecret; }
    public String getZalopaySecret() { return zalopaySecret; }
    public void setZalopaySecret(String zalopaySecret) { this.zalopaySecret = zalopaySecret; }
    public String getCarrierSecret() { return carrierSecret; }
    public void setCarrierSecret(String carrierSecret) { this.carrierSecret = carrierSecret; }
}
