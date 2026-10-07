package com.dtthuan3.ecommerce.catalogservice.service;

import com.dtthuan3.ecommerce.catalogservice.entity.OutboxEvent;

public interface OutboxEventService {

    OutboxEvent createEvent(
            String aggregateType,
            Long aggregateId,
            String eventType,
            String payload
    );
}