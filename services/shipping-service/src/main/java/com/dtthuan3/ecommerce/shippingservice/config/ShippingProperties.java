package com.dtthuan3.ecommerce.shippingservice.config;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Cau hinh fake carrier (GHN-like). Secret chi tu env/secret. */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "shipping")
public class ShippingProperties {
    private String publicBaseUrl = "http://localhost:8089";
    private Duration quoteTtl = Duration.ofMinutes(15);
    private String fakeGatewayUrl = "http://localhost:8099";
    private Carrier fakeGhn = new Carrier();

    @Getter
    @Setter
    public static class Carrier {
        private String secret = "local_fake_carrier_change_me";
    }
}
