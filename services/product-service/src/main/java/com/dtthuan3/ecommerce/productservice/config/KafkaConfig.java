package com.dtthuan3.ecommerce.productservice.config;

import com.dtthuan3.ecommerce.productservice.constant.KafkaTopic;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Value("${app.kafka.topic.partitions:3}")
    private int partitions;

    @Value("${app.kafka.topic.replication-factor:1}")
    private short replicationFactor;

    @Bean
    public NewTopic productEventsTopic() {
        return TopicBuilder.name(KafkaTopic.PRODUCT_EVENTS)
                .partitions(partitions)
                .replicas(replicationFactor)
                .build();
    }

    @Bean
    public NewTopic productEventsDltTopic() {
        return TopicBuilder.name(KafkaTopic.PRODUCT_EVENTS_DLT)
                .partitions(partitions)
                .replicas(replicationFactor)
                .build();
    }
}