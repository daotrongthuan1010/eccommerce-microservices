package com.dtthuan3.ecommerce.orderservice.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

/**
 * §issue #5: retry + DLT cho consumer payment-events cua order-service.
 * PaymentEventConsumer rethrow khi xu ly loi — khong co error handler thi
 * poison message block partition vo han. Day la phan cau hinh con thieu
 * ma code review 2026-11-07 dinh danh.
 *
 * <p>Spring Boot tu dong inject bean CommonErrorHandler vao container factory
 * mac dinh (cung pattern InventoryKafkaConfig dang dung tren production).
 */
@Slf4j
@Configuration
public class OrderKafkaConfig {

    @Bean
    public DefaultErrorHandler orderKafkaErrorHandler(
            KafkaTemplate<String, String> kafkaTemplate) {
        // §7 backend-conventions: DLT theo quy uoc <topic>.DLT — ap dung cho
        // moi topic consumer cua service nay, khong hardcode tung topic
        DeadLetterPublishingRecoverer publisher = new DeadLetterPublishingRecoverer(kafkaTemplate,
                (record, failure) -> new TopicPartition(record.topic() + ".DLT", record.partition()));
        // Coi cuu ho thanh cong chi khi broker xac nhan publish DLT
        publisher.setFailIfSendResultIsError(true);

        // 3 lan retry cach 1s roi moi DLT — khong khoa partition vo han
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(publisher,
                new FixedBackOff(1_000L, 3L));
        errorHandler.setCommitRecovered(true);
        return errorHandler;
    }
}
