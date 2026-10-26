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
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
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
    private final AtomicInteger inFlightEvents = new AtomicInteger();

    @Value("${inventory.kafka.outbox-batch-size:100}")
    private int batchSize;

    @Value("${inventory.kafka.outbox-max-in-flight:100}")
    private int maxInFlight;

    @Value("${inventory.kafka.outbox-lease:PT3M}")
    private Duration leaseDuration;

    @Value("${inventory.kafka.outbox-retention:PT720H}")
    private Duration outboxRetention;

    @PostConstruct
    void registerMetrics() {
        meterRegistry.gauge("inventory.event.outbox.pending", pendingEvents);
        meterRegistry.gauge("inventory.event.outbox.oldest_age_seconds", oldestEventAgeSeconds);
        meterRegistry.gauge("inventory.event.outbox.in_flight", inFlightEvents);
        if (maxInFlight < 1) {
            throw new IllegalStateException("inventory.kafka.outbox-max-in-flight phải lớn hơn 0");
        }
    }

    /** Claim ngắn trong PostgreSQL, sau đó gửi bất đồng bộ để không giữ transaction/row lock qua mạng. */
    @Scheduled(fixedDelayString = "${inventory.kafka.outbox-poll-delay-ms:500}")
    public void publishAvailableBatch() {
        Instant now = Instant.now();
        // Chỉ claim số lượng publisher còn năng lực xử lý; backlog lớn không tạo vô hạn request gửi Kafka.
        int requestedSlots = Math.min(Math.max(batchSize, 1), maxInFlight);
        int reservedSlots = reserveSendSlots(requestedSlots);
        if (reservedSlots == 0) return;

        List<OutboxMessage> batch;
        try {
            int safeBatchSize = Math.max(1, Math.min(batchSize, 500));
            batch = outboxRepository.claimAvailable(Math.min(reservedSlots, safeBatchSize), now,
                    now.plus(leaseDuration));
        } catch (RuntimeException failure) {
            inFlightEvents.addAndGet(-reservedSlots);
            meterRegistry.counter("inventory.event.outbox.claim.failures").increment();
            log.error("Không claim được Inventory event outbox", failure);
            return;
        }

        // Nếu backlog ít hơn số slot đã giữ, trả slot thừa để lần poll sau có thể xử lý ngay.
        int unusedSlots = reservedSlots - batch.size();
        if (unusedSlots > 0) inFlightEvents.addAndGet(-unusedSlots);

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
                        try {
                            if (failure == null) {
                                recordPublishDuration(message, publishTimer, "ack");
                                markPublished(message, result);
                            } else {
                                recordPublishDuration(message, publishTimer, "failure");
                                scheduleRetry(message, failure);
                            }
                        } catch (RuntimeException callbackFailure) {
                            // Nếu xử lý callback lỗi, lease sẽ hết hạn và publisher poll lại event sau đó.
                            meterRegistry.counter("inventory.event.outbox.callback.failures").increment();
                            log.error("Lỗi xử lý callback Inventory outbox, eventId={}",
                                    message.eventId(), callbackFailure);
                        } finally {
                            inFlightEvents.decrementAndGet();
                        }
                    });
        } catch (RuntimeException failure) {
            try {
                recordPublishDuration(message, publishTimer, "failure");
                scheduleRetry(message, failure);
            } finally {
                inFlightEvents.decrementAndGet();
            }
        }
    }

    /** Giữ slot bằng CAS để không vượt giới hạn ngay cả khi scheduler chạy song song. */
    private int reserveSendSlots(int requested) {
        if (requested < 1) return 0;
        while (true) {
            int current = inFlightEvents.get();
            int available = maxInFlight - current;
            if (available <= 0) return 0;
            int reserved = Math.min(requested, available);
            if (inFlightEvents.compareAndSet(current, current + reserved)) return reserved;
        }
    }

    private void recordPublishDuration(OutboxMessage message, Timer.Sample sample, String outcome) {
        sample.stop(Timer.builder("inventory.event.outbox.send.duration")
                .tag("event_type", message.eventType()).tag("outcome", outcome).register(meterRegistry));
    }

    private void markPublished(OutboxMessage message, SendResult<String, String> result) {
        try {
            if (outboxRepository.markPublished(message.eventId(), message.leaseToken(), Instant.now())) {
                meterRegistry.counter("inventory.event.outbox.published",
                        "event_type", message.eventType()).increment();
                log.debug("Đã publish Inventory event: eventId={}, topic={}, partition={}, offset={}",
                        message.eventId(), message.topic(), result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            } else {
                meterRegistry.counter("inventory.event.outbox.stale_callbacks", "operation", "ack").increment();
                log.debug("Bỏ ACK callback không còn sở hữu lease: eventId={}", message.eventId());
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
        long baseDelaySeconds = Math.min(300, 1L << shift);
        // Rải các lần retry để nhiều event lỗi cùng lúc không dồn thành một đợt gửi lại.
        long jitterRangeSeconds = Math.max(1, baseDelaySeconds / 4);
        long jitterSeconds = ThreadLocalRandom.current().nextLong(-jitterRangeSeconds,
                jitterRangeSeconds + 1);
        long delaySeconds = Math.max(1, Math.min(300, baseDelaySeconds + jitterSeconds));
        try {
            boolean scheduled = outboxRepository.scheduleRetry(message.eventId(), message.leaseToken(),
                    Instant.now().plusSeconds(delaySeconds), rootMessage(failure));
            if (scheduled) {
                meterRegistry.counter("inventory.event.outbox.publish.failures",
                        "event_type", message.eventType()).increment();
                log.warn("Publish Inventory event lỗi; retry sau {} giây, eventId={}, attempt={}",
                        delaySeconds, message.eventId(), message.attempts(), failure);
            } else {
                meterRegistry.counter("inventory.event.outbox.stale_callbacks", "operation", "retry").increment();
                log.debug("Bỏ retry callback không còn sở hữu lease: eventId={}", message.eventId());
            }
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
