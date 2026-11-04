package com.dtthuan3.ecommerce.inventoryservice.client;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * Cấu hình RestClient nội bộ có load balancing và timeout cho các lần gọi Catalog/Product.
 * Class này đặt ranh giới giao tiếp với Product Service/projection cục bộ, cấu hình cách tạo client và giới
 * hạn thời gian gọi để lỗi phụ thuộc ngoài không chờ vô hạn.
 */
@Configuration
public class ProductSkuClientConfig {

    @Bean
    @LoadBalanced
    RestClient.Builder productServiceRestClientBuilder(
            @Value("${inventory.integration.product-connect-timeout}") Duration connectTimeout,
            @Value("${inventory.integration.product-read-timeout}") Duration readTimeout) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeout);
        requestFactory.setReadTimeout(readTimeout);
        return RestClient.builder().requestFactory(requestFactory);
    }
}
