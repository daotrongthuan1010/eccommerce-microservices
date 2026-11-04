package com.dtthuan3.ecommerce.paymentservice.config;

import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/** RestClient goi fake-gateway: timeout chat (connect 2s, read 3s) de provider cham khong treo thread cua order. */
@Configuration
public class RestClientConfig {
    @Bean
    RestClient fakeGatewayRestClient(PaymentProperties props) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(2));
        factory.setReadTimeout(Duration.ofSeconds(3));
        return RestClient.builder().baseUrl(props.getFakeGatewayUrl()).requestFactory(factory).build();
    }
}
