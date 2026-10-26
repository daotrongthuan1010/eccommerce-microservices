package com.dtthuan3.ecommerce.catalogservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.util.backoff.FixedBackOff;
import org.springframework.beans.factory.annotation.Value;

@Configuration
@EnableKafka
public class CatalogSearchKafkaConfig {

    @Bean
    public CommonErrorHandler productSearchProjectionErrorHandler(KafkaTemplate<String, String> kafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (record, exception) -> new TopicPartition("product-search-projection.DLT", record.partition()));
        return new DefaultErrorHandler(recoverer, new FixedBackOff(1_000L, 3L));
    }

    @Bean
    public org.apache.kafka.clients.admin.NewTopic productSearchProjectionDltTopic(
            @Value("${catalog.search.kafka.replication-factor:1}") short replicationFactor) {
        return TopicBuilder.name("product-search-projection.DLT").partitions(3).replicas(replicationFactor).build();
    }
}
