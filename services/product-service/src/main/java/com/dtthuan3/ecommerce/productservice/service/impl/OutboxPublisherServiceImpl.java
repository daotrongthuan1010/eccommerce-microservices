package com.dtthuan3.ecommerce.productservice.service.impl;

import com.dtthuan3.ecommerce.productservice.constant.OutboxStatus;
import com.dtthuan3.ecommerce.productservice.domain.OutboxEvent;
import com.dtthuan3.ecommerce.productservice.repository.OutboxEventRepository;
import com.dtthuan3.ecommerce.productservice.service.OutboxPublisherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisherServiceImpl implements OutboxPublisherService {

    private static final int MAX_RETRIES = 10;

    private static final long SEND_TIMEOUT_SECONDS = 5;

    private static final long INITIAL_RETRY_DELAY_SECONDS = 5;

    private static final long MAX_RETRY_DELAY_SECONDS = 300;

    private static final String HEADER_EVENT_ID = "event-id";

    private static final String HEADER_EVENT_TYPE = "event-type";

    private static final String HEADER_EVENT_VERSION = "event-version";

    private final OutboxEventRepository outboxEventRepository;

    private final KafkaTemplate<String, String> kafkaTemplate;

    @Override
    @Transactional
    public void publish(UUID eventRowId) {

        OutboxEvent event = outboxEventRepository
                .findByIdForUpdate(eventRowId)
                .orElse(null);

        if (event == null || event.getStatus() != OutboxStatus.PENDING) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        if (event.getNextRetryAt() != null
                && event.getNextRetryAt().isAfter(now)) {
            return;
        }

        ProducerRecord<String, String> record =
                createProducerRecord(event);

        SendResult<String, String> result;

        try {

            result = kafkaTemplate.send(record)
                    .get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        } catch (InterruptedException exception) {

            Thread.currentThread().interrupt();

            handleFailure(event, exception);

            return;

        } catch (Exception exception) {

            handleFailure(event, exception);

            return;
        }

        handleSuccess(event, result);
    }

    /**
     * Tạo Kafka message bao gồm:
     * - Topic
     * - Key
     * - Payload
     * - Event metadata headers
     */
    private ProducerRecord<String, String> createProducerRecord(
            OutboxEvent event
    ) {

        ProducerRecord<String, String> record =
                new ProducerRecord<>(
                        event.getTopic(),
                        event.getAggregateId().toString(),
                        event.getPayload()
                );

        // Event ID: định danh duy nhất của event.
        record.headers().add(
                HEADER_EVENT_ID,
                event.getEventId()
                        .toString()
                        .getBytes(StandardCharsets.UTF_8)
        );

        // Event type: loại business event.
        record.headers().add(
                HEADER_EVENT_TYPE,
                event.getEventType()
                        .getBytes(StandardCharsets.UTF_8)
        );

        // Event version: phiên bản schema.
        record.headers().add(
                HEADER_EVENT_VERSION,
                String.valueOf(event.getEventVersion())
                        .getBytes(StandardCharsets.UTF_8)
        );

        return record;
    }

    /**
     * Xử lý khi Kafka xác nhận đã nhận message.
     */
    private void handleSuccess(
            OutboxEvent event,
            SendResult<String, String> result
    ) {

        event.setStatus(OutboxStatus.PUBLISHED);

        event.setPublishedAt(LocalDateTime.now());

        event.setNextRetryAt(null);

        outboxEventRepository.save(event);

        log.info(
                "Published outbox event successfully: " +
                        "eventId={}, eventType={}, topic={}, partition={}, offset={}",
                event.getEventId(),
                event.getEventType(),
                event.getTopic(),
                result.getRecordMetadata().partition(),
                result.getRecordMetadata().offset()
        );
    }

    /**
     * Xử lý khi publish Kafka thất bại.
     *
     * Retry theo exponential backoff.
     */
    private void handleFailure(
            OutboxEvent event,
            Exception exception
    ) {

        int retryCount = event.getRetryCount() == null
                ? 0
                : event.getRetryCount();

        retryCount++;

        event.setRetryCount(retryCount);

        // 1. Đã hết số lần retry.
        if (retryCount >= MAX_RETRIES) {

            event.setStatus(OutboxStatus.FAILED);

            event.setNextRetryAt(null);

            log.error(
                    "Outbox event permanently failed: " +
                            "eventId={}, eventType={}, topic={}, retries={}",
                    event.getEventId(),
                    event.getEventType(),
                    event.getTopic(),
                    retryCount,
                    exception
            );

        } else {

            // 2. Vẫn còn lượt retry.
            event.setStatus(OutboxStatus.PENDING);

            long delaySeconds = calculateRetryDelay(retryCount);

            LocalDateTime nextRetryAt =
                    LocalDateTime.now().plusSeconds(delaySeconds);

            event.setNextRetryAt(nextRetryAt);

            log.warn(
                    "Failed to publish outbox event: " +
                            "eventId={}, eventType={}, topic={}, " +
                            "retry={}/{}, nextRetryAt={}",
                    event.getEventId(),
                    event.getEventType(),
                    event.getTopic(),
                    retryCount,
                    MAX_RETRIES,
                    nextRetryAt,
                    exception
            );
        }

        outboxEventRepository.save(event);
    }

    /**
     * Exponential backoff:
     *
     * Retry 1: 5 giây
     * Retry 2: 10 giây
     * Retry 3: 20 giây
     * Retry 4: 40 giây
     * Retry 5: 80 giây
     * Retry 6: 160 giây
     * Retry 7 trở đi: tối đa 300 giây
     */
    private long calculateRetryDelay(int retryCount) {

        long multiplier = 1L << Math.min(retryCount - 1, 6);

        return Math.min(
                INITIAL_RETRY_DELAY_SECONDS * multiplier,
                MAX_RETRY_DELAY_SECONDS
        );
    }
}