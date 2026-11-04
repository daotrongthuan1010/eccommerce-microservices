package com.dtthuan3.ecommerce.catalogservice.service.impl;

import com.dtthuan3.ecommerce.catalogservice.dto.search.SearchDltReplayResponse;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

/** Replays one quarantined record without moving the main consumer group's offsets. */
@Service
@RequiredArgsConstructor
public class ProductSearchDltReplayService {

    private static final String DLT_TOPIC = "product-search-projection.DLT";
    private final KafkaProperties kafkaProperties;
    private final ProductSearchProjectionListener projectionListener;

    public SearchDltReplayResponse replay(int partition, long offset) {
        if (partition < 0 || offset < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "partition/offset phải không âm");
        }
        Map<String, Object> properties = kafkaProperties.buildConsumerProperties();
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, "catalog-search-dlt-replay-" + UUID.randomUUID());
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        TopicPartition topicPartition = new TopicPartition(DLT_TOPIC, partition);

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(properties)) {
            consumer.assign(java.util.List.of(topicPartition));
            consumer.seek(topicPartition, offset);
            long deadline = System.nanoTime() + Duration.ofSeconds(5).toNanos();
            while (System.nanoTime() < deadline) {
                for (ConsumerRecord<String, String> record : consumer.poll(Duration.ofMillis(250))) {
                    if (record.offset() == offset) {
                        projectionListener.onProductEvent(record.value());
                        return new SearchDltReplayResponse(DLT_TOPIC, partition, offset, "REPLAYED");
                    }
                    if (record.offset() > offset) break;
                }
            }
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Không tìm thấy record DLT tại partition/offset yêu cầu");
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Replay thất bại; record vẫn còn trong DLT để xử lý lại", ex);
        }
    }
}
