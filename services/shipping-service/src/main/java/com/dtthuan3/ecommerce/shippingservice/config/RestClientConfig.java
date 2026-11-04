package com.dtthuan3.ecommerce.shippingservice.config;

import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/** RestClient goi fake carrier: timeout chat de carrier cham khong treo luong tao van don. */
@Configuration
public class RestClientConfig {
    @Bean
    RestClient fakeCarrierRestClient(ShippingProperties props) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(2));
        factory.setReadTimeout(Duration.ofSeconds(3));
        return RestClient.builder().baseUrl(props.getFakeGatewayUrl()).requestFactory(factory).build();
    }
}
