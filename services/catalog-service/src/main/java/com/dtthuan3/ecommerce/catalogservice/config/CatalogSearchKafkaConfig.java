
package com.dtthuan3.ecommerce.catalogservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
@EnableKafka
public class CatalogSearchKafkaConfig {

    @Bean
    public CommonErrorHandler productSearchProjectionErrorHandler(
            KafkaTemplate<String, String> kafkaTemplate,
            @Value("${catalog.search.events.dlt-topic}") String dltTopic) {

        DeadLetterPublishingRecoverer recoverer =
                new DeadLetterPublishingRecoverer(
                        kafkaTemplate,
                        (record, exception) ->
                                new TopicPartition(dltTopic, record.partition())
                );

        return new DefaultErrorHandler(
                recoverer,
                new FixedBackOff(1_000L, 3L)
        );
    }

    @Bean
    public NewTopic productSearchProjectionDltTopic(
            @Value("${catalog.search.kafka.replication-factor:1}")
            short replicationFactor,
            @Value("${catalog.search.kafka.dlt-partitions:3}")
            int partitions,
            @Value("${catalog.search.events.dlt-topic}")
            String dltTopic) {

        return TopicBuilder
                .name(dltTopic)
                .partitions(partitions)
                .replicas(replicationFactor)
                .build();
    }
}
