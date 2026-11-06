package com.dtthuan3.ecommerce.searchservice.config;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * RestClient goi product-service qua Eureka service discovery (load balancer
 * resolver cua spring-cloud). Base url la ten service — khong hardcode IP.
 */
@Configuration
public class HttpClientConfig {

    @Bean
    public RestClient productServiceRestClient(
            @Value("${search.product-service.base-url:http://product-service}") String baseUrl
    ) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factoryWithTimeouts())
                .build();
    }

    private static org.springframework.http.client.ClientHttpRequestFactory factoryWithTimeouts() {
        var factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(10));
        return factory;
    }
}
