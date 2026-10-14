package com.dtthuan3.ecommerce.catalogservice.service;

import com.fasterxml.jackson.core.type.TypeReference;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public interface RedisCacheService {

    <T> Optional<T> get(
            String key,
            Class<T> type
    );

    <T> Optional<T> get(
            String key,
            TypeReference<T> type
    );

    <T> List<T> getList(
            String key,
            Class<T> elementType
    );

    void set(
            String key,
            Object value
    );

    void set(
            String key,
            Object value,
            Duration ttl
    );

    void delete(
            String key
    );

    void deleteByPattern(
            String pattern
    );

    boolean hasKey(
            String key
    );

    <T> T getOrLoad(
            String key,
            Class<T> type,
            Duration ttl,
            Supplier<T> loader
    );

    <T> List<T> getOrLoadList(
            String key,
            Class<T> elementType,
            Duration ttl,
            Supplier<List<T>> loader
    );

    <T> Map<String, T> getOrLoadMap(
            String key,
            Duration ttl,
            Supplier<Map<String, T>> loader
    );
}
