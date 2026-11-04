package com.dtthuan3.ecommerce.inventoryservice.config;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.RetryListener;
import org.springframework.util.backoff.FixedBackOff;
import java.time.Duration;

/**
 * Cấu hình các thành phần Kafka dùng riêng trong Inventory: retry, chuyển event lỗi sang DLT và khai báo
 * topic cần tồn tại để Spring Kafka tạo lúc ứng dụng khởi động. Tên topic, partition và replication factor
 * được đọc từ YAML để mỗi môi trường điều chỉnh mà không phải sửa logic nghiệp vụ.
 */
@Slf4j
@Configuration
public class InventoryKafkaConfig {

    /**
     * Khai báo topic nhận sự kiện duyệt hàng trả từ Order/Return service.
     * KafkaAdmin tạo topic nếu chưa có và giữ nguyên cấu hình hiện tại nếu topic đã tồn tại.
     */
    @Bean
    @ConditionalOnProperty(prefix = "inventory.kafka.topic-provisioning", name = "enabled",
            havingValue = "true", matchIfMissing = true)
    public NewTopic inventoryReturnApprovalTopic(
            @Value("${inventory.kafka.return-approval-events-topic}") String topicName,
            @Value("${inventory.kafka.topic-provisioning.partitions}") int partitions,
            @Value("${inventory.kafka.topic-provisioning.replication-factor}") short replicationFactor) {
        return TopicBuilder.name(topicName)
                .partitions(partitions)
                .replicas(replicationFactor)
                .build();
    }

    /**
     * Khai báo dead-letter topic cùng số partition với topic gốc để giữ nguyên partition khi chuyển event lỗi.
     */
    @Bean
    @ConditionalOnProperty(prefix = "inventory.kafka.topic-provisioning", name = "enabled",
            havingValue = "true", matchIfMissing = true)
    public NewTopic inventoryReturnApprovalDeadLetterTopic(
            @Value("${inventory.kafka.return-approval-events-dlt-topic}") String topicName,
            @Value("${inventory.kafka.topic-provisioning.partitions}") int partitions,
            @Value("${inventory.kafka.topic-provisioning.replication-factor}") short replicationFactor) {
        return TopicBuilder.name(topicName)
                .partitions(partitions)
                .replicas(replicationFactor)
                .build();
    }

    @Bean
    public DefaultErrorHandler inventoryKafkaErrorHandler(
            KafkaTemplate<String, String> kafkaTemplate,
            MeterRegistry meterRegistry,
            @Value("${inventory.kafka.product-events-topic}") String productTopic,
            @Value("${inventory.kafka.product-events-dlt-topic}") String productDeadLetterTopic,
            @Value("${inventory.kafka.return-approval-events-topic}") String returnTopic,
            @Value("${inventory.kafka.return-approval-events-dlt-topic}")
            String returnDeadLetterTopic,
            @Value("${inventory.kafka.consumer-retry-interval}") Duration retryInterval,
            @Value("${inventory.kafka.consumer-retry-attempts}") long retryAttempts) {
        // Chuyển event hỏng về DLT cùng hợp đồng topic; giữ nguyên partition để phục vụ đối soát.
        DeadLetterPublishingRecoverer publisher = new DeadLetterPublishingRecoverer(kafkaTemplate,
                (record, failure) -> new TopicPartition(record.topic().equals(productTopic)
                        ? productDeadLetterTopic : record.topic().equals(returnTopic)
                        ? returnDeadLetterTopic : record.topic() + ".DLT", record.partition()));
        // Không coi cứu hộ thành công nếu broker chưa xác nhận việc publish vào DLT.
        publisher.setFailIfSendResultIsError(true);

        // Lỗi xử lý được thử lại theo backoff hữu hạn; sau đó mới chuyển DLT để không khóa partition vô thời hạn.
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(publisher,
                new FixedBackOff(retryInterval.toMillis(), retryAttempts));
        errorHandler.setRetryListeners(new RetryListener() {
            @Override
            public void failedDelivery(ConsumerRecord<?, ?> record, Exception failure, int deliveryAttempt) {
                meterRegistry.counter("inventory.kafka.consumer.retries",
                        "consumer", "inventory-events",
                        "topic", record.topic()).increment();
                log.warn("Inventory Kafka event xử lý lỗi, listener sẽ retry: topic={}, partition={}, offset={}, attempt={}",
                        record.topic(), record.partition(), record.offset(), deliveryAttempt, failure);
            }

            @Override
            public void recovered(ConsumerRecord<?, ?> record, Exception failure) {
                meterRegistry.counter("inventory.kafka.consumer.dead_letter",
                        "consumer", "inventory-events",
                        "topic", record.topic()).increment();
                log.error("Đã gửi Inventory event sang DLT sau khi retry hết: topic={}, partition={}, offset={}",
                        record.topic(), record.partition(), record.offset(), failure);
            }

            @Override
            public void recoveryFailed(ConsumerRecord<?, ?> record, Exception original, Exception recoveryFailure) {
                meterRegistry.counter("inventory.kafka.consumer.dead_letter.failures",
                        "consumer", "inventory-events",
                        "topic", record.topic()).increment();
                log.error("Gửi Inventory event sang DLT thất bại; record chưa được xác nhận đã cứu hộ: "
                                + "topic={}, partition={}, offset={}",
                        record.topic(), record.partition(), record.offset(), recoveryFailure);
            }
        });
        // Chỉ cho phép commit offset của record đã được recover thành công; nếu publish DLT lỗi, record còn retry.
        errorHandler.setCommitRecovered(true);
        return errorHandler;
    }
}
