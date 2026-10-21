package com.dtthuan3.ecommerce.inventoryservice.config;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

/** Retry có giới hạn rồi chuyển record lỗi sang DLT để không chặn mãi partition Product events. */
@Slf4j
@Configuration
public class InventoryKafkaConfig {

    @Bean
    public DefaultErrorHandler inventoryKafkaErrorHandler(
            KafkaTemplate<String, String> kafkaTemplate,
            MeterRegistry meterRegistry,
            @Value("${inventory.kafka.product-events-dlt-topic:product-events.DLT}") String deadLetterTopic) {
        DeadLetterPublishingRecoverer publisher = new DeadLetterPublishingRecoverer(kafkaTemplate,
                (record, failure) -> new TopicPartition(deadLetterTopic, record.partition()));
        publisher.setFailIfSendResultIsError(true);

        DefaultErrorHandler errorHandler = new DefaultErrorHandler((ConsumerRecord<?, ?> record, Exception failure) -> {
            meterRegistry.counter("inventory.kafka.consumer.dead_letter",
                    "consumer", "product-sku-projection").increment();
            log.error("Đưa Product event sang DLT sau khi retry hết: topic={}, partition={}, offset={}",
                    record.topic(), record.partition(), record.offset(), failure);
            publisher.accept(record, failure);
        }, new FixedBackOff(1000L, 3L));
        errorHandler.setCommitRecovered(true);
        return errorHandler;
    }
}
