
package com.dtthuan3.ecommerce.catalogservice.kafka.consumer;

import com.dtthuan3.ecommerce.catalogservice.kafka.event.ProductPublishedEvent;
import com.dtthuan3.ecommerce.catalogservice.kafka.handler.ProductEventHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductEventConsumer {

    private final ObjectMapper objectMapper;
    private final ProductEventHandler productEventHandler;

    /**
     * Kiểm tra Spring có khởi tạo Consumer Bean hay không.
     */
    @PostConstruct
    public void init() {
        log.info("========== PRODUCT EVENT CONSUMER BEAN INITIALIZED ==========");
        log.info("Product event consumer is ready");
    }

    /**
     * Nhận Product Event từ Kafka.
     */
    @KafkaListener(
            id = "productEventListener",
            topics = "${catalog.search.events.product-topic:product-events}",
            groupId = "${catalog.search.events.group-id:catalog-product-search-projection}",
            concurrency = "${catalog.search.events.concurrency:3}"
    )
    public void consume(ConsumerRecord<String, String> record) {

        // Kiểm tra phương thức có thực sự nhận được message không.
        System.err.println("========== CONSUME METHOD IS RUNNING ==========");

        String key = record.key();
        String message = record.value();

        log.info(
                "Received product event: topic={}, partition={}, offset={}, key={}",
                record.topic(),
                record.partition(),
                record.offset(),
                key
        );

        if (message == null || message.isBlank()) {
            log.error(
                    "Product event message is empty: topic={}, partition={}, offset={}",
                    record.topic(),
                    record.partition(),
                    record.offset()
            );

            throw new IllegalArgumentException(
                    "Product event message must not be empty"
            );
        }

        try {
            // Bước 1: Deserialize JSON thành ProductPublishedEvent.
            ProductPublishedEvent event = objectMapper.readValue(
                    message,
                    ProductPublishedEvent.class
            );

            log.info(
                    "Deserialized product event: eventId={}, eventType={}, productId={}, revisionId={}",
                    event.eventId(),
                    event.eventType(),
                    event.productId(),
                    event.revisionId()
            );

            // Bước 2: Validate event.
            if (event.eventId() == null) {
                throw new IllegalArgumentException(
                        "Product event eventId must not be null"
                );
            }

            if (event.eventType() == null || event.eventType().isBlank()) {
                throw new IllegalArgumentException(
                        "Product event eventType must not be empty"
                );
            }

            if (event.productId() == null) {
                throw new IllegalArgumentException(
                        "Product event productId must not be null"
                );
            }

            // Bước 3: Gọi Handler xử lý event.
            log.info(
                    "Calling ProductEventHandler: productId={}, eventType={}",
                    event.productId(),
                    event.eventType()
            );

            productEventHandler.handle(event);

            // Bước 4: Xử lý thành công.
            log.info(
                    "Product event processed successfully: eventId={}, productId={}, eventType={}",
                    event.eventId(),
                    event.productId(),
                    event.eventType()
            );

        } catch (JsonProcessingException e) {
            log.error(
                    "JSON DESERIALIZATION FAILED: topic={}, partition={}, offset={}, key={}, message={}",
                    record.topic(),
                    record.partition(),
                    record.offset(),
                    key,
                    message,
                    e
            );

            throw new IllegalArgumentException(
                    "Cannot deserialize ProductPublishedEvent",
                    e
            );

        } catch (Exception e) {
            log.error(
                    "PRODUCT EVENT PROCESSING FAILED: topic={}, partition={}, offset={}, key={}",
                    record.topic(),
                    record.partition(),
                    record.offset(),
                    key,
                    e
            );

            // Ném lại exception để Spring Kafka xử lý retry/DLT.
            throw new IllegalStateException(
                    "Failed to process product event",
                    e
            );
        }
    }
}
