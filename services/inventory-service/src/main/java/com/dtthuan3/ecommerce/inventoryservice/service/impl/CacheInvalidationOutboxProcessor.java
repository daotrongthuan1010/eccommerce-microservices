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

import java.time.Duration;
import java.time.Instant;

/** Applies durable cache invalidation commands and retains failures for retry. */
@Slf4j
@Service
@RequiredArgsConstructor
public class CacheInvalidationOutboxProcessor {
    private final CacheInvalidationOutboxRepository outboxRepository;
    private final CacheManager cacheManager;
    private final MeterRegistry meterRegistry;

    @Transactional
    public void process(String eventId) {
        CacheInvalidationOutboxEvent event = outboxRepository.findByIdForUpdate(eventId).orElse(null);
        if (event == null || event.getAvailableAt().isAfter(Instant.now())) return;

        try {
            Cache cache = cacheManager.getCache(event.getCacheName());
            if (cache == null) throw new IllegalStateException("Unknown cache " + event.getCacheName());
            cache.evictIfPresent(event.getCacheKey());
            outboxRepository.delete(event);
            meterRegistry.counter("inventory.cache.invalidation.processed", "cache", event.getCacheName()).increment();
        } catch (RuntimeException failure) {
            long delaySeconds = Math.min(300, 1L << Math.min(event.getAttempts(), 8));
            event.retryAt(Instant.now().plus(Duration.ofSeconds(delaySeconds)));
            outboxRepository.save(event);
            meterRegistry.counter("inventory.cache.invalidation.failures", "cache", event.getCacheName()).increment();
            log.error("Cache invalidation outbox event {} failed; retry scheduled in {} seconds (attempt {})",
                    event.getId(), delaySeconds, event.getAttempts(), failure);
        }
    }
}
