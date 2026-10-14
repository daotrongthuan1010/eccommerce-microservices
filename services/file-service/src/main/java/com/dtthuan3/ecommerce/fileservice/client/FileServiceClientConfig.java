package com.dtthuan3.ecommerce.fileservice.client;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Bean dung chung cho FileServiceClient (goi file-service qua Eureka + LoadBalancer).
 * Tach rieng file de IntelliJ khong do do 2 class chung 1 file.
 */
@Configuration
public class FileServiceClientConfig {

    @Bean
    @LoadBalanced
    RestClient.Builder loadBalancedRestClientBuilder() {
        return RestClient.builder();
    }
}
