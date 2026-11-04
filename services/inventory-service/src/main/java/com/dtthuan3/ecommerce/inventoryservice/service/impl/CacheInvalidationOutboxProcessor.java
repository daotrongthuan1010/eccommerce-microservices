package com.dtthuan3.ecommerce.inventoryservice.service.impl;

import com.dtthuan3.ecommerce.inventoryservice.entity.CacheInvalidationOutboxEvent;
import com.dtthuan3.ecommerce.inventoryservice.repository.CacheInvalidationOutboxRepository;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;

import java.time.Duration;
import java.time.Instant;
import jakarta.annotation.PostConstruct;

/**
 * Xử lý lệnh invalidation đã commit; giữ lại và retry nếu Redis chưa thể xóa cache.
 * Class này hiện thực use case hoặc tác vụ nền của Inventory, phối hợp kiểm tra nghiệp vụ, repository và
 * tích hợp cần thiết để hoàn tất một bước xử lý; adapter bên ngoài không tự thao tác dữ liệu.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CacheInvalidationOutboxProcessor {
    private final CacheInvalidationOutboxRepository outboxRepository;
    private final CacheManager cacheManager;
    private final MeterRegistry meterRegistry;

    @Value("${inventory.cache.outbox-retry-base-delay}")
    private Duration retryBaseDelay;

    @Value("${inventory.cache.outbox-retry-max-delay}")
    private Duration retryMaxDelay;

    @Value("${inventory.cache.outbox-retry-max-exponent}")
    private int retryMaxExponent;

    /** Kiểm tra cấu hình retry trước khi worker bắt đầu nhận lệnh xóa cache. */
    @PostConstruct
    void validateRetryConfiguration() {
        if (retryBaseDelay.isNegative() || retryBaseDelay.isZero()
                || retryMaxDelay.compareTo(retryBaseDelay) < 0 || retryMaxExponent < 0) {
            throw new IllegalStateException("Cấu hình retry cache outbox không hợp lệ");
        }
    }

    @Transactional
    public void process(String eventId) {
        // Khóa event trong transaction để hai poller không cùng xử lý; lệnh chưa đến hạn được giữ nguyên để lần
        // poll sau đọc lại.
        CacheInvalidationOutboxEvent event = outboxRepository.findByIdForUpdate(eventId).orElse(null);
        if (event == null || event.getAvailableAt().isAfter(Instant.now())) return;

        try {
            Cache cache = cacheManager.getCache(event.getCacheName());
            if (cache == null) throw new IllegalStateException("Unknown cache " + event.getCacheName());
            // Key * chỉ dùng cho cache danh sách Location có nhiều key phân trang.
            if ("*".equals(event.getCacheKey())) {
                // Chỉ clear namespace Location phân trang đã được đăng ký; CacheWriter dùng SCAN batch, không quét KEYS
                // đồng bộ.
                cache.clear();
            } else {
                cache.evictIfPresent(event.getCacheKey());
            }
            // Chỉ xóa lệnh sau khi Redis eviction hoàn tất; nếu DB commit thất bại, xóa lặp lại vẫn an toàn.
            outboxRepository.delete(event);
            meterRegistry.counter("inventory.cache.invalidation.processed", "cache", event.getCacheName()).increment();
        } catch (RuntimeException failure) {
            // Redis và PostgreSQL không có distributed transaction; giữ event, tăng attempts và đặt lịch retry có
            // backoff.
            long exponent = Math.min(event.getAttempts(), retryMaxExponent);
            long baseMillis = retryBaseDelay.toMillis();
            long maxMillis = retryMaxDelay.toMillis();
            long multiplier = 1L << Math.min(exponent, 30);
            long delayMillis = baseMillis > maxMillis / multiplier
                    ? maxMillis : baseMillis * multiplier;
            event.retryAt(Instant.now().plusMillis(delayMillis));
            outboxRepository.save(event);
            meterRegistry.counter("inventory.cache.invalidation.failures", "cache", event.getCacheName()).increment();
            log.error("Cache invalidation outbox event {} failed; retry scheduled in {} seconds (attempt {})",
                    event.getId(), Duration.ofMillis(delayMillis).toSeconds(), event.getAttempts(), failure);
        }
    }
}
