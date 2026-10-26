package com.dtthuan3.ecommerce.catalogservice.service;

import com.dtthuan3.ecommerce.catalogservice.entity.OutboxEvent;

import java.util.Map;

public interface OutboxEventService {

    OutboxEvent createEvent(
            String aggregateType,
            String aggregateId,
            Long aggregateVersion,
            String eventType,
            Map<String, Object> payload
    );
}