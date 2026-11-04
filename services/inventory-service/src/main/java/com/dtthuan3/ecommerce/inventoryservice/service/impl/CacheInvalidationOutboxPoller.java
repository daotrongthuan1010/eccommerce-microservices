package com.dtthuan3.ecommerce.inventoryservice.service.impl;

import com.dtthuan3.ecommerce.inventoryservice.repository.CacheInvalidationOutboxRepository;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;
import jakarta.annotation.PostConstruct;

/**
 * Định kỳ lấy lệnh vô hiệu hóa cache đã commit từ outbox và chuyển cho processor để xóa cache.
 * Mỗi lần poll cập nhật metric số lệnh đang chờ và tuổi lệnh cũ nhất; claim/lock tại repository
 * cho phép nhiều instance chạy song song mà không xử lý cùng một lệnh đồng thời.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CacheInvalidationOutboxPoller {
    private final CacheInvalidationOutboxRepository outboxRepository;
    private final CacheInvalidationOutboxProcessor processor;
    private final MeterRegistry meterRegistry;
    private final AtomicLong pendingEvents = new AtomicLong();
    private final AtomicLong oldestEventAgeSeconds = new AtomicLong();

    @PostConstruct
    void registerMetrics() {
        meterRegistry.gauge("inventory.cache.invalidation.pending", pendingEvents);
        meterRegistry.gauge("inventory.cache.invalidation.oldest_age_seconds", oldestEventAgeSeconds);
    }

    @Scheduled(fixedDelayString = "${inventory.cache.outbox-poll-delay-ms}")
    public void poll() {
        try {
            // Chỉ lấy lệnh đến hạn, theo thứ tự tạo và giới hạn batch; processor khóa từng ID để các instance phối hợp
            // an toàn.
            var batch = outboxRepository.findTop100ByAvailableAtLessThanEqualOrderByCreatedAtAsc(Instant.now());
            batch.forEach(event -> processor.process(event.getId()));
        } catch (RuntimeException failure) {
            meterRegistry.counter("inventory.cache.outbox.poll.errors").increment();
            log.error("Không đọc được cache invalidation outbox", failure);
        }
    }

    @Scheduled(fixedDelayString = "${inventory.cache.outbox-metrics-refresh-ms}")
    public void refreshBacklogMetrics() {
        try {
            pendingEvents.set(outboxRepository.count());
            oldestEventAgeSeconds.set(outboxRepository.findFirstByOrderByCreatedAtAsc()
                    .map(event -> Math.max(0, Instant.now().getEpochSecond() - event.getCreatedAt().getEpochSecond()))
                    .orElse(0L));
        } catch (RuntimeException failure) {
            meterRegistry.counter("inventory.cache.outbox.poll.errors").increment();
            log.error("Không cập nhật được metric cache invalidation outbox", failure);
        }
    }
}
