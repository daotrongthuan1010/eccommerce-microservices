
package com.dtthuan3.ecommerce.productservice.service.impl;

import com.dtthuan3.ecommerce.productservice.constant.OutboxStatus;
import com.dtthuan3.ecommerce.productservice.domain.OutboxEvent;
import com.dtthuan3.ecommerce.productservice.repository.OutboxEventRepository;
import com.dtthuan3.ecommerce.productservice.service.OutboxPublisherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisherServiceImpl implements OutboxPublisherService {

    private static final int MAX_RETRIES = 10;
    private static final long SEND_TIMEOUT_SECONDS = 5;

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

        try {
            kafkaTemplate.send(
                    event.getTopic(),
                    event.getAggregateId().toString(),
                    event.getPayload()
            ).get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            event.setStatus(OutboxStatus.PUBLISHED);
            event.setPublishedAt(LocalDateTime.now());
            event.setNextRetryAt(null);

            outboxEventRepository.save(event);

            log.info(
                    "Published outbox event: eventId={}, type={}, topic={}",
                    event.getEventId(),
                    event.getEventType(),
                    event.getTopic()
            );

        } catch (Exception exception) {
            handleFailure(event, exception);
        }
    }

    private void handleFailure(
            OutboxEvent event,
            Exception exception
    ) {
        int retryCount = event.getRetryCount() == null
                ? 0
                : event.getRetryCount();

        retryCount++;
        event.setRetryCount(retryCount);

        if (retryCount >= MAX_RETRIES) {
            event.setStatus(OutboxStatus.FAILED);
            event.setNextRetryAt(null);

            log.error(
                    "Outbox event permanently failed: eventId={}, retries={}",
                    event.getEventId(),
                    retryCount,
                    exception
            );
        } else {
            event.setStatus(OutboxStatus.PENDING);

            long delaySeconds = Math.min(
                    5L * (1L << Math.min(retryCount - 1, 6)),
                    300L
            );

            event.setNextRetryAt(
                    LocalDateTime.now().plusSeconds(delaySeconds)
            );

            log.warn(
                    "Failed to publish outbox event: eventId={}, retry={}/{}, nextRetryAt={}",
                    event.getEventId(),
                    retryCount,
                    MAX_RETRIES,
                    event.getNextRetryAt(),
                    exception
            );
        }

        outboxEventRepository.save(event);
    }
}
