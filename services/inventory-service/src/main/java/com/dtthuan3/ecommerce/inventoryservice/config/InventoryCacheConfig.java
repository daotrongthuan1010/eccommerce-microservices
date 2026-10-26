package com.dtthuan3.ecommerce.inventoryservice.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCache;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import lombok.RequiredArgsConstructor;

import java.time.Duration;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import org.springframework.data.redis.connection.RedisServerCommands;
import org.springframework.boot.actuate.metrics.cache.RedisCacheMetrics;

/** Cấu hình Redis cache cho metadata Warehouse và Location ít thay đổi. */
@Slf4j
@Configuration
@EnableCaching
@EnableScheduling
@RequiredArgsConstructor
public class InventoryCacheConfig extends CachingConfigurerSupport {

    private final RedisConnectionFactory connectionFactory;

    @Value("${inventory.cache.location-id-ttl:PT2M}")
    private Duration locationIdCacheTtl;

    @Value("${inventory.cache.location-list-ttl:PT2M}")
    private Duration locationListCacheTtl;

    @Value("${inventory.cache.warehouse-id-ttl:PT5M}")
    private Duration warehouseIdCacheTtl;

    @Value("${inventory.cache.warehouse-list-ttl:PT3M}")
    private Duration warehouseListCacheTtl;

    @Value("${inventory.cache.availability-ttl:PT1S}")
    private Duration availabilityCacheTtl;

    private final MeterRegistry meterRegistry;

    /**
     * RedisCacheManager dùng key chuỗi và value JSON để nhiều instance Inventory chia sẻ cache.
     * TTL giới hạn dữ liệu cũ; các thay đổi ghi lệnh invalidation vào outbox cùng transaction PostgreSQL.
     */
    @Bean
    @Override
    public CacheManager cacheManager() {
        RedisCacheConfiguration configuration = cacheConfiguration(locationIdCacheTtl);
        RedisCacheConfiguration warehouseIdConfiguration = cacheConfiguration(warehouseIdCacheTtl);
        RedisCacheConfiguration warehouseListConfiguration = cacheConfiguration(warehouseListCacheTtl);
        RedisCacheConfiguration locationListConfiguration = cacheConfiguration(locationListCacheTtl);
        // Thêm độ lệch 0–25% để các snapshot không hết hạn đồng loạt và tạo nhiều cache miss cùng lúc.
        RedisCacheConfiguration availabilityConfiguration = cacheConfiguration(availabilityCacheTtl)
                .entryTtl((key, value) -> availabilityCacheTtl.plusMillis(
                        ThreadLocalRandom.current().nextLong(1, Math.max(2, availabilityCacheTtl.toMillis() / 4))));

        RedisCacheManager manager = RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(configuration)
                .withInitialCacheConfigurations(Map.of(
                        "inventory-location-by-id-v2", configuration,
                        "inventory-location-list-v1", locationListConfiguration,
                        "inventory-availability-by-sku-v1", availabilityConfiguration,
                        "inventory-warehouse-by-id-v1", warehouseIdConfiguration,
                        "inventory-warehouse-list-by-enterprise-v1", warehouseListConfiguration))
                .enableStatistics()
                .build();
        // RedisCache statistics expose shared-cache hit/miss counters through Micrometer/Prometheus.
        manager.getCacheNames().forEach(name -> {
            if (manager.getCache(name) instanceof RedisCache cache) {
                new RedisCacheMetrics(cache, Tags.of("cache", name)).bindTo(meterRegistry);
            }
        });
        meterRegistry.gauge("inventory.redis.used_memory.bytes", connectionFactory,
                factory -> {
                    try {
                        try (var connection = factory.getConnection()) {
                            RedisServerCommands commands = connection.serverCommands();
                            Properties memory = commands.info("memory");
                            return memory == null ? Double.NaN
                                    : Double.parseDouble(memory.getProperty("used_memory", "NaN"));
                        }
                    } catch (RuntimeException ex) {
                        return Double.NaN;
                    }
                });
        return manager;
    }

    /** Chia sẻ serializer/key format giữa các cache, chỉ khác TTL theo tính chất dữ liệu. */
    private RedisCacheConfiguration cacheConfiguration(Duration ttl) {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(ttl)
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(
                        new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(
                        new GenericJackson2JsonRedisSerializer()));
    }

    /**
     * Redis chỉ tăng tốc đọc nên lỗi cache không được làm hỏng nghiệp vụ: lỗi GET để Spring đọc PostgreSQL,
     * lỗi PUT/evict được ghi log và dữ liệu cũ (nếu có) tự hết hạn theo TTL.
     */
    @Bean
    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
                meterRegistry.counter("inventory.redis.errors", "operation", "get").increment();
                log.warn("Redis không đọc được cache '{}' với key '{}'; tiếp tục đọc từ PostgreSQL",
                        cache.getName(), key, exception);
            }

            @Override
            public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
                meterRegistry.counter("inventory.redis.errors", "operation", "put").increment();
                log.warn("Redis không ghi được cache '{}' với key '{}'; bỏ qua thao tác cache",
                        cache.getName(), key, exception);
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
                retryEviction(cache, key, exception);
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, Cache cache) {
                meterRegistry.counter("inventory.redis.errors", "operation", "clear").increment();
                log.warn("Không cho phép clear cache rộng; cache '{}' tự hết hạn theo TTL", cache.getName(), exception);
            }

            private void retryEviction(Cache cache, Object key, RuntimeException initialFailure) {
                meterRegistry.counter("inventory.cache.invalidation.failures", "cache", cache.getName()).increment();
                RuntimeException lastFailure = initialFailure;
                for (int attempt = 1; attempt <= 3; attempt++) {
                    try {
                        Thread.sleep(50L * attempt);
                        cache.evictIfPresent(key);
                        meterRegistry.counter("inventory.cache.invalidation.retries", "cache", cache.getName()).increment();
                        return;
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        lastFailure = initialFailure;
                        break;
                    } catch (RuntimeException retryFailure) {
                        lastFailure = retryFailure;
                    }
                }
                log.error("Redis invalidation thất bại sau retry cho cache '{}' key '{}'; dữ liệu cũ có thể tồn tại tới TTL",
                        cache.getName(), key, lastFailure);
            }
        };
    }
}
