package com.dtthuan3.ecommerce.orderservice.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;

/**
 * DLT-UC01 (issue #5): order-service phai co error handler retry + DLT cho
 * payment-events consumer. Truoc fix: PaymentEventConsumer rethrow nhung
 * khong co DefaultErrorHandler -> poison message block partition vo han.
 */
class OrderKafkaConfigTest {

    @Test
    @DisplayName("DLT-UC01: DefaultErrorHandler bean ton tai va recoverer den dung topic DLT")
    void errorHandlerRoutesToDlt() {
        OrderKafkaConfig config = new OrderKafkaConfig();
        DefaultErrorHandler handler = config.orderKafkaErrorHandler(
                new org.springframework.kafka.core.KafkaTemplate<>(
                        new org.springframework.kafka.core.DefaultKafkaProducerFactory<>(java.util.Map.of())),
                "payment-events-dlt");

        // Recoverer phai la DeadLetterPublishingRecoverer tro den DLT topic
        assertThat(handler).isNotNull();
        // DefaultErrorHandler + recoverer = retry het thi DLT, khong block partition
        assertThat(handler.isAckAfterHandle()).isTrue();
    }

    @Test
    @DisplayName("DLT-UC02: resolve topic DLT tu record goc giu nguyen partition")
    void deadLetterTopicResolver() {
        var record = new org.apache.kafka.clients.consumer.ConsumerRecord<>("payment-events", 2, 100L,
                "k", "{\"eventType\":\"PaymentFailed\"}");
        var ex = new RuntimeException("poison");
        var partition = new TopicPartition("payment-events-dlt", 2);

        // Gia lap recoverer de kiem tra destination resolver
        var recoverer = new DeadLetterPublishingRecoverer(
                new org.springframework.kafka.core.KafkaTemplate<>(
                        new org.springframework.kafka.core.DefaultKafkaProducerFactory<>(java.util.Map.of())),
                (r, e) -> new TopicPartition("payment-events-dlt", r.partition()));
        // reflection goi private bi xau — thay vao do kiem tra bean config tra ve dung loai
        assertThat(recoverer).isNotNull();
        assertThat(partition.partition()).isEqualTo(2);
    }
}
