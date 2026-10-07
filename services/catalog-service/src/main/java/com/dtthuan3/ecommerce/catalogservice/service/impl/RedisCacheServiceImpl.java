package com.dtthuan3.ecommerce.catalogservice.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.dtthuan3.ecommerce.catalogservice.service.RedisCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class RedisCacheServiceImpl implements RedisCacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public <T> Optional<T> get(String key, Class<T> type) {

        Object value = redisTemplate.opsForValue().get(key);

        if (value == null) {
            return Optional.empty();
        }

        return Optional.of(objectMapper.convertValue(value, type));
    }

    @Override
    public <T> Optional<T> get(
            String key,
            TypeReference<T> type
    ) {

        Object value = redisTemplate.opsForValue().get(key);

        if (value == null) {
            return Optional.empty();
        }

        return Optional.of(
                objectMapper.convertValue(value, type)
        );
    }

    @Override
    public <T> List<T> getList(
            String key,
            Class<T> elementType
    ) {

        Object value = redisTemplate.opsForValue().get(key);

        if (value == null) {
            return Collections.emptyList();
        }

        return objectMapper.convertValue(
                value,
                objectMapper.getTypeFactory()
                        .constructCollectionType(List.class, elementType)
        );
    }

    @Override
    public void set(
            String key,
            Object value
    ) {

        redisTemplate.opsForValue().set(key, value);
    }

    @Override
    public void set(String key, Object value, Duration ttl) {

        redisTemplate.opsForValue().set(key, value, ttl);
    }

    @Override
    public void delete(String key) {

        redisTemplate.delete(key);
    }

    @Override
    public void deleteByPattern(String pattern) {

        Set<String> keys = redisTemplate.keys(pattern);

        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }


    @Override
    public boolean hasKey(String key) {

        return Boolean.TRUE.equals(
                redisTemplate.hasKey(key)
        );
    }


    @Override
    public <T> T getOrLoad(String key, Class<T> type, Duration ttl, Supplier<T> loader) {

        Optional<T> cached = get(key, type);

        if (cached.isPresent()) {
            return cached.get();
        }

        T value = loader.get();

        if (value != null) {
            set(key, value, ttl);
        }

        return value;
    }

    @Override
    public <T> List<T> getOrLoadList(
            String key,
            Class<T> elementType,
            Duration ttl,
            Supplier<List<T>> loader
    ) {

        List<T> cached = getList(key, elementType);

        if (!cached.isEmpty() || hasKey(key)) {
            return cached;
        }

        List<T> value = loader.get();

        if (value != null) {
            set(key, value, ttl);
            return value;
        }

        return Collections.emptyList();
    }

    @Override
    public <T> Map<String, T> getOrLoadMap(
            String key,
            Duration ttl,
            Supplier<Map<String, T>> loader
    ) {

        Optional<Map<String, T>> cached = get(
                key,
                new TypeReference<Map<String, T>>() {}
        );

        if (cached.isPresent()) {
            return cached.get();
        }

        Map<String, T> value = loader.get();

        if (value != null) {
            set(key, value, ttl);
            return value;
        }

        return Collections.emptyMap();
    }
}