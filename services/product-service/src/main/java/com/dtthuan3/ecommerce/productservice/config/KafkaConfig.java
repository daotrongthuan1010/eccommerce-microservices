package com.dtthuan3.ecommerce.productservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic productEventsTopic() {
        return new NewTopic(
                "product-events",
                3,
                (short) 1
        );
    }
}