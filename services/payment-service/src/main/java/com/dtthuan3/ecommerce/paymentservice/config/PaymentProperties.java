package com.dtthuan3.ecommerce.paymentservice.config;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Cau hinh provider + fake-gateway. Secret chi lay tu env/secret, khong hardcode trong code. */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "payment")
public class PaymentProperties {
    private String publicBaseUrl = "http://localhost:8086";
    private Duration pendingTtl = Duration.ofMinutes(15);
    private Duration reconcileAfter = Duration.ofMinutes(5);
    private Gateway vnpay = new Gateway();
    private Gateway zalopay = new Gateway();
    private String fakeGatewayUrl = "http://localhost:8099";

    @Getter
    @Setter
    public static class Gateway {
        private String secret = "local_fake_change_me";
        private String scenario = "";
    }
}
