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
import org.springframework.kafka.listener.RetryListener;
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

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(publisher, new FixedBackOff(1000L, 3L));
        errorHandler.setRetryListeners(new RetryListener() {
            @Override
            public void failedDelivery(ConsumerRecord<?, ?> record, Exception failure, int deliveryAttempt) {
                meterRegistry.counter("inventory.kafka.consumer.retries",
                        "consumer", "product-sku-projection",
                        "topic", record.topic()).increment();
                log.warn("Product event xử lý lỗi, listener sẽ retry: topic={}, partition={}, offset={}, attempt={}",
                        record.topic(), record.partition(), record.offset(), deliveryAttempt, failure);
            }

            @Override
            public void recovered(ConsumerRecord<?, ?> record, Exception failure) {
                meterRegistry.counter("inventory.kafka.consumer.dead_letter",
                        "consumer", "product-sku-projection",
                        "topic", record.topic()).increment();
                log.error("Đã gửi Product event sang DLT sau khi retry hết: topic={}, partition={}, offset={}",
                        record.topic(), record.partition(), record.offset(), failure);
            }

            @Override
            public void recoveryFailed(ConsumerRecord<?, ?> record, Exception original, Exception recoveryFailure) {
                meterRegistry.counter("inventory.kafka.consumer.dead_letter.failures",
                        "consumer", "product-sku-projection",
                        "topic", record.topic()).increment();
                log.error("Gửi Product event sang DLT thất bại; record chưa được xác nhận đã cứu hộ: "
                                + "topic={}, partition={}, offset={}",
                        record.topic(), record.partition(), record.offset(), recoveryFailure);
            }
        });
        errorHandler.setCommitRecovered(true);
        return errorHandler;
    }
}
