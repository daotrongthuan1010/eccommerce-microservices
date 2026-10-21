package com.dtthuan3.ecommerce.inventoryservice.service.impl;

import com.dtthuan3.ecommerce.inventoryservice.repository.InventoryEventOutboxRepository;
import com.dtthuan3.ecommerce.inventoryservice.repository.InventoryEventOutboxRepository.OutboxMessage;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/** Phát outbox sau commit; lease cho phép nhiều instance poll mà không khóa hàng trong lúc gọi Kafka. */
@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryEventOutboxPublisher {
    private final InventoryEventOutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final MeterRegistry meterRegistry;
    private final AtomicLong pendingEvents = new AtomicLong();
    private final AtomicLong oldestEventAgeSeconds = new AtomicLong();

    @Value("${inventory.kafka.outbox-batch-size:100}")
    private int batchSize;

    @Value("${inventory.kafka.outbox-lease:PT3M}")
    private Duration leaseDuration;

    @Value("${inventory.kafka.outbox-retention:PT720H}")
    private Duration outboxRetention;

    @PostConstruct
    void registerMetrics() {
        meterRegistry.gauge("inventory.event.outbox.pending", pendingEvents);
        meterRegistry.gauge("inventory.event.outbox.oldest_age_seconds", oldestEventAgeSeconds);
    }

    /** Claim ngắn trong PostgreSQL, sau đó gửi bất đồng bộ để không giữ transaction/row lock qua mạng. */
    @Scheduled(fixedDelayString = "${inventory.kafka.outbox-poll-delay-ms:500}")
    public void publishAvailableBatch() {
        Instant now = Instant.now();
        List<OutboxMessage> batch;
        try {
            int safeBatchSize = Math.max(1, Math.min(batchSize, 500));
            batch = outboxRepository.claimAvailable(safeBatchSize, now, now.plus(leaseDuration));
        } catch (RuntimeException failure) {
            meterRegistry.counter("inventory.event.outbox.claim.failures").increment();
            log.error("Không claim được Inventory event outbox", failure);
            return;
        }

        if (!batch.isEmpty()) {
            meterRegistry.counter("inventory.event.outbox.claimed").increment(batch.size());
        }
        for (OutboxMessage message : batch) {
            send(message);
        }
    }

    /** Cập nhật gauge backlog; metric lấy ở chu kỳ riêng để không thêm COUNT vào mỗi lần poll. */
    @Scheduled(fixedDelayString = "${inventory.kafka.outbox-metrics-refresh-ms:10000}")
    public void refreshBacklogMetrics() {
        try {
            pendingEvents.set(outboxRepository.countPending());
            oldestEventAgeSeconds.set(outboxRepository.oldestPendingCreatedAt()
                    .map(createdAt -> Math.max(0, Duration.between(createdAt, Instant.now()).getSeconds()))
                    .orElse(0L));
        } catch (RuntimeException failure) {
            meterRegistry.counter("inventory.event.outbox.metrics.failures").increment();
            log.error("Không cập nhật được metric Inventory event outbox", failure);
        }
    }

    /** Chỉ xóa event đã broker xác nhận và quá retention; event pending không bị tự loại khỏi hàng đợi. */
    @Scheduled(fixedDelayString = "${inventory.kafka.outbox-cleanup-delay-ms:86400000}")
    public void cleanupPublishedEvents() {
        try {
            int deleted = outboxRepository.deletePublishedBefore(Instant.now().minus(outboxRetention));
            if (deleted > 0) {
                meterRegistry.counter("inventory.event.outbox.cleanup.deleted").increment(deleted);
                log.info("Đã xóa {} Inventory outbox event quá retention {}", deleted, outboxRetention);
            }
        } catch (RuntimeException failure) {
            meterRegistry.counter("inventory.event.outbox.cleanup.failures").increment();
            log.error("Không dọn được Inventory event outbox đã publish", failure);
        }
    }

    private void send(OutboxMessage message) {
        Timer.Sample publishTimer = Timer.start(meterRegistry);
        try {
            kafkaTemplate.send(message.topic(), message.aggregateId(), message.payload())
                    .whenComplete((result, failure) -> {
                        if (failure == null) {
                            recordPublishDuration(message, publishTimer, "ack");
                            markPublished(message, result);
                        } else {
                            recordPublishDuration(message, publishTimer, "failure");
                            scheduleRetry(message, failure);
                        }
                    });
        } catch (RuntimeException failure) {
            recordPublishDuration(message, publishTimer, "failure");
            scheduleRetry(message, failure);
        }
    }

    private void recordPublishDuration(OutboxMessage message, Timer.Sample sample, String outcome) {
        sample.stop(Timer.builder("inventory.event.outbox.send.duration")
                .tag("event_type", message.eventType()).tag("outcome", outcome).register(meterRegistry));
    }

    private void markPublished(OutboxMessage message, SendResult<String, String> result) {
        try {
            if (outboxRepository.markPublished(message.eventId(), Instant.now())) {
                meterRegistry.counter("inventory.event.outbox.published",
                        "event_type", message.eventType()).increment();
                log.debug("Đã publish Inventory event: eventId={}, topic={}, partition={}, offset={}",
                        message.eventId(), message.topic(), result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        } catch (RuntimeException failure) {
            // Nếu broker đã nhận nhưng DB chưa được đánh dấu, lease sẽ hết hạn và event được gửi lại.
            // Consumer phải khử trùng lặp bằng eventId; không thể ghi trạng thái publish trong callback này.
            meterRegistry.counter("inventory.event.outbox.ack_persistence.failures").increment();
            log.error("Kafka đã nhận event nhưng không lưu được published_at; eventId={}",
                    message.eventId(), failure);
        }
    }

    private void scheduleRetry(OutboxMessage message, Throwable failure) {
        long shift = Math.min(Math.max(message.attempts() - 1, 0), 9);
        long delaySeconds = Math.min(300, 1L << shift);
        try {
            outboxRepository.scheduleRetry(message.eventId(), Instant.now().plusSeconds(delaySeconds),
                    rootMessage(failure));
            meterRegistry.counter("inventory.event.outbox.publish.failures",
                    "event_type", message.eventType()).increment();
            log.warn("Publish Inventory event lỗi; retry sau {} giây, eventId={}, attempt={}",
                    delaySeconds, message.eventId(), message.attempts(), failure);
        } catch (RuntimeException persistenceFailure) {
            meterRegistry.counter("inventory.event.outbox.retry_persistence.failures").increment();
            log.error("Không lưu được lịch retry Inventory outbox; lease sẽ hết hạn, eventId={}",
                    message.eventId(), persistenceFailure);
        }
    }

    private String rootMessage(Throwable failure) {
        Throwable cause = failure;
        while (cause.getCause() != null) cause = cause.getCause();
        String message = cause.getMessage();
        return message == null ? cause.getClass().getSimpleName() : message;
    }
}
