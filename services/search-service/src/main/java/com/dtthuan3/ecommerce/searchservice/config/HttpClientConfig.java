package com.dtthuan3.ecommerce.searchservice.config;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * RestClient goi product-service QUA EUREKA (ten service http://product-service
 * → IP pod). Spring Cloud can @LoadBalanced tren RestClient.Builder de cai
 * LoadBalancerInterceptor resolve ten service — khong co no la "Connect timed
 * out" vi DNS khong biet ten service (chi Service k8s moi co DNS).
 */
@Configuration
public class HttpClientConfig {

    @Bean
    @LoadBalanced
    public RestClient.Builder loadBalancedRestClientBuilder() {
        return RestClient.builder()
                .requestFactory(factoryWithTimeouts());
    }

    @Bean
    public RestClient productServiceRestClient(
            @Qualifier("loadBalancedRestClientBuilder") RestClient.Builder builder,
            @Value("${search.product-service.base-url:http://product-service}") String baseUrl
    ) {
        return builder.baseUrl(baseUrl).build();
    }

    private static ClientHttpRequestFactory factoryWithTimeouts() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(10));
        return factory;
    }
}
