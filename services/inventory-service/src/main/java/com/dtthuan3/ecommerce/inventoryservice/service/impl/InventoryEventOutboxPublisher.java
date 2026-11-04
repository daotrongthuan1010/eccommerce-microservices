package com.dtthuan3.ecommerce.inventoryservice.service.impl;

import com.dtthuan3.ecommerce.inventoryservice.repository.InventoryEventOutboxRepository;
import com.dtthuan3.ecommerce.inventoryservice.repository.InventoryEventOutboxRepository.OutboxMessage;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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

/**
 * Phát outbox sau commit; lease cho phép nhiều instance poll mà không khóa hàng trong lúc gọi Kafka.
 * Class này hiện thực use case hoặc tác vụ nền của Inventory, phối hợp kiểm tra nghiệp vụ, repository và
 * tích hợp cần thiết để hoàn tất một bước xử lý; adapter bên ngoài không tự thao tác dữ liệu.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "inventory.kafka.outbox.publisher", name = "enabled",
        havingValue = "true", matchIfMissing = true)
public class InventoryEventOutboxPublisher {
    private final InventoryEventOutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final MeterRegistry meterRegistry;
    private final AtomicLong pendingEvents = new AtomicLong();
    private final AtomicLong oldestEventAgeSeconds = new AtomicLong();
    private final AtomicInteger inFlightEvents = new AtomicInteger();

    @Value("${inventory.kafka.outbox-batch-size}")
    private int batchSize;

    @Value("${inventory.kafka.outbox-max-in-flight}")
    private int maxInFlight;

    @Value("${inventory.kafka.outbox-lease}")
    private Duration leaseDuration;

    @Value("${inventory.kafka.outbox-retention}")
    private Duration outboxRetention;

    @Value("${inventory.kafka.outbox-retry-base-delay}")
    private Duration retryBaseDelay;

    @Value("${inventory.kafka.outbox-retry-max-delay}")
    private Duration retryMaxDelay;

    @Value("${inventory.kafka.outbox-retry-max-exponent}")
    private int retryMaxExponent;

    @Value("${inventory.kafka.outbox-cleanup-batch-size}")
    private int outboxCleanupBatchSize;

    @Value("${inventory.kafka.outbox-cleanup-max-batches}")
    private int outboxCleanupMaxBatches;

    @Value("${inventory.kafka.outbox-cleanup-batch-limit}")
    private int outboxCleanupBatchLimit;

    @Value("${inventory.kafka.outbox-cleanup-max-batches-per-run}")
    private int outboxCleanupMaxBatchesPerRun;

    @Value("${inventory.kafka.outbox-claim-batch-limit}")
    private int outboxClaimBatchLimit;

    @PostConstruct
    void registerMetrics() {
        meterRegistry.gauge("inventory.event.outbox.pending", pendingEvents);
        meterRegistry.gauge("inventory.event.outbox.oldest_age_seconds", oldestEventAgeSeconds);
        meterRegistry.gauge("inventory.event.outbox.in_flight", inFlightEvents);
        if (maxInFlight < 1) {
            throw new IllegalStateException("inventory.kafka.outbox-max-in-flight phải lớn hơn 0");
        }
        if (batchSize < 1 || outboxClaimBatchLimit < 1 || outboxCleanupBatchSize < 1
                || outboxCleanupMaxBatches < 1 || outboxCleanupBatchLimit < 1
                || outboxCleanupMaxBatchesPerRun < 1) {
            throw new IllegalStateException("Cấu hình batch của Inventory event outbox phải lớn hơn 0");
        }
        if (retryBaseDelay.isNegative() || retryBaseDelay.isZero()
                || retryMaxDelay.compareTo(retryBaseDelay) < 0 || retryMaxExponent < 0) {
            throw new IllegalStateException("Cấu hình retry outbox không hợp lệ; cần base > 0, max >= base, exponent >= 0");
        }
    }

    /** Claim ngắn trong PostgreSQL, sau đó gửi bất đồng bộ để không giữ transaction/row lock qua mạng. */
    @Scheduled(fixedDelayString = "${inventory.kafka.outbox-poll-delay-ms}")
    public void publishAvailableBatch() {
        Instant now = Instant.now();
        // Giữ chỗ trước khi claim để số Future đang bay không vượt cấu hình, dù scheduler có lần chạy chồng nhau.
        // Backlog lớn được tiêu thụ theo nhiều batch thay vì tạo vô hạn request Kafka và gây áp lực heap/network.
        int requestedSlots = Math.min(Math.max(batchSize, 1), maxInFlight);
        int reservedSlots = reserveSendSlots(requestedSlots);
        if (reservedSlots == 0) return;

        List<OutboxMessage> batch;
        try {
            int safeBatchSize = Math.max(1, Math.min(batchSize, outboxClaimBatchLimit));
            batch = outboxRepository.claimAvailable(Math.min(reservedSlots, safeBatchSize), now,
                    now.plus(leaseDuration));
        } catch (RuntimeException failure) {
            inFlightEvents.addAndGet(-reservedSlots);
            meterRegistry.counter("inventory.event.outbox.claim.failures").increment();
            log.error("Không claim được Inventory event outbox", failure);
            return;
        }

        // Nếu backlog ít hơn số slot giữ trước, trả slot thừa ngay để lần poll sau không bị chặn giả bởi giới hạn.
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
    @Scheduled(fixedDelayString = "${inventory.kafka.outbox-metrics-refresh-ms}")
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
    @Scheduled(fixedDelayString = "${inventory.kafka.outbox-cleanup-delay-ms}")
    public void cleanupPublishedEvents() {
        try {
            Instant cutoff = Instant.now().minus(outboxRetention);
            int safeBatchSize = Math.max(1, Math.min(outboxCleanupBatchSize, outboxCleanupBatchLimit));
            int safeMaxBatches = Math.max(1, Math.min(outboxCleanupMaxBatches, outboxCleanupMaxBatchesPerRun));
            int deleted = 0;
            // Mỗi câu lệnh xóa bị giới hạn; tổng số batch trong một lượt cũng có trần để không chiếm DB quá lâu.
            for (int batch = 0; batch < safeMaxBatches; batch++) {
                int deletedInBatch = outboxRepository.deletePublishedBefore(cutoff, safeBatchSize);
                deleted += deletedInBatch;
                if (deletedInBatch < safeBatchSize) break;
            }
            if (deleted > 0) {
                meterRegistry.counter("inventory.event.outbox.cleanup.deleted").increment(deleted);
                log.info("Đã xóa {} Inventory outbox event quá retention {} theo các lô tối đa {} dòng",
                        deleted, outboxRetention, safeBatchSize);
            }
        } catch (RuntimeException failure) {
            meterRegistry.counter("inventory.event.outbox.cleanup.failures").increment();
            log.error("Không dọn được Inventory event outbox đã publish", failure);
        }
    }

    private void send(OutboxMessage message) {
        Timer.Sample publishTimer = Timer.start(meterRegistry);
        try {
            // Transaction claim đã kết thúc trước khi gọi Kafka; không giữ row lock PostgreSQL trong lúc chờ mạng.
            kafkaTemplate.send(message.topic(), message.aggregateId(), message.payload())
                    .whenComplete((result, failure) -> {
                        try {
                            if (failure == null) {
                                recordPublishDuration(message, publishTimer, "ack");
                                // ACK broker rồi mới đánh dấu published trong DB; nếu DB lỗi, lease hết hạn sẽ gửi lại.
                                markPublished(message, result);
                            } else {
                                recordPublishDuration(message, publishTimer, "failure");
                                scheduleRetry(message, failure);
                            }
                        } catch (RuntimeException callbackFailure) {
                            // Nếu xử lý callback lỗi, không xóa event; lease hết hạn để publisher khác thử lại.
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
            // Có cửa sổ không thể loại bỏ giữa ACK của Kafka và ghi published_at vào PostgreSQL.
            // Event có thể được gửi lại sau lease, nên consumer phải khử trùng lặp bằng eventId/inbox.
            meterRegistry.counter("inventory.event.outbox.ack_persistence.failures").increment();
            log.error("Kafka đã nhận event nhưng không lưu được published_at; eventId={}",
                    message.eventId(), failure);
        }
    }

    private void scheduleRetry(OutboxMessage message, Throwable failure) {
        // Exponential backoff và trần thời gian lấy từ YAML; jitter phân tán các event lỗi khi broker phục hồi.
        long shift = Math.min(Math.max(message.attempts() - 1, 0), Math.min(retryMaxExponent, 30));
        long maxDelayMillis = retryMaxDelay.toMillis();
        long multiplier = 1L << shift;
        long configuredBaseMillis = retryBaseDelay.toMillis();
        long baseDelayMillis = configuredBaseMillis > maxDelayMillis / multiplier
                ? maxDelayMillis : configuredBaseMillis * multiplier;
        long jitterRangeMillis = Math.max(1, baseDelayMillis / 4);
        long jitterMillis = ThreadLocalRandom.current().nextLong(-jitterRangeMillis,
                jitterRangeMillis + 1);
        long jitteredDelayMillis = jitterMillis > 0 && baseDelayMillis > Long.MAX_VALUE - jitterMillis
                ? maxDelayMillis : baseDelayMillis + jitterMillis;
        long delayMillis = Math.max(1, Math.min(maxDelayMillis, jitteredDelayMillis));
        try {
            boolean scheduled = outboxRepository.scheduleRetry(message.eventId(), message.leaseToken(),
                    Instant.now().plusMillis(delayMillis), rootMessage(failure));
            if (scheduled) {
                meterRegistry.counter("inventory.event.outbox.publish.failures",
                        "event_type", message.eventType()).increment();
                log.warn("Publish Inventory event lỗi; retry sau {} giây, eventId={}, attempt={}",
                        Duration.ofMillis(delayMillis).toSeconds(), message.eventId(), message.attempts(), failure);
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
