package com.dtthuan3.ecommerce.inventoryservice.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import lombok.RequiredArgsConstructor;

import java.time.Duration;

/** Cấu hình Redis cache riêng cho các truy vấn Location ít thay đổi. */
@Slf4j
@Configuration
@EnableCaching
@RequiredArgsConstructor
public class InventoryCacheConfig extends CachingConfigurerSupport {

    private final RedisConnectionFactory connectionFactory;

    @Value("${inventory.cache.location-ttl:PT5M}")
    private Duration locationCacheTtl;

    /**
     * RedisCacheManager lưu key dạng chuỗi và value dạng JSON để nhiều instance Inventory dùng chung cache.
     * TTL giới hạn thời gian tồn tại của dữ liệu cũ; transactionAware hoãn thao tác cache đến khi DB commit.
     */
    @Bean
    @Override
    public CacheManager cacheManager() {
        RedisCacheConfiguration configuration = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(locationCacheTtl)
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(
                        new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(
                        new GenericJackson2JsonRedisSerializer()));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(configuration)
                .transactionAware()
                .build();
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
                log.warn("Redis không đọc được cache '{}' với key '{}'; tiếp tục đọc từ PostgreSQL",
                        cache.getName(), key, exception);
            }

            @Override
            public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
                log.warn("Redis không ghi được cache '{}' với key '{}'; bỏ qua thao tác cache",
                        cache.getName(), key, exception);
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
                log.warn("Redis không xóa được cache '{}' với key '{}'; dữ liệu cache sẽ hết hạn theo TTL",
                        cache.getName(), key, exception);
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, Cache cache) {
                log.warn("Redis không thể xóa toàn bộ cache '{}'; dữ liệu cache sẽ hết hạn theo TTL",
                        cache.getName(), exception);
            }
        };
    }
}
