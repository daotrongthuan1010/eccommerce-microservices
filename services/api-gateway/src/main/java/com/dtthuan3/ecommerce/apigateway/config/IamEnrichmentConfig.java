package com.dtthuan3.ecommerce.apigateway.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableConfigurationProperties(IamEnrichmentProperties.class)
public class IamEnrichmentConfig {

    @Bean
    @LoadBalanced
    WebClient.Builder iamWebClientBuilder() {
        return WebClient.builder();
    }
}
