package com.dtthuan3.ecommerce.catalogservice.service.impl;

import com.dtthuan3.ecommerce.catalogservice.contstant.OutboxEventStatus;
import com.dtthuan3.ecommerce.catalogservice.entity.OutboxEvent;
import com.dtthuan3.ecommerce.catalogservice.repository.OutboxEventRepository;
import com.dtthuan3.ecommerce.catalogservice.service.OutboxEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OutboxEventServiceImpl
        implements OutboxEventService {

    private static final String PRODUCER = "catalog-service";

    private static final Integer EVENT_SCHEMA_VERSION = 1;

    private final OutboxEventRepository outboxEventRepository;

    @Override
    public OutboxEvent createEvent(
            String aggregateType,
            String aggregateId,
            Long aggregateVersion,
            String eventType,
            Map<String, Object> payload
    ) {

        LocalDateTime now = LocalDateTime.now();

        OutboxEvent event = OutboxEvent.builder()
                .aggregateType(aggregateType)
                .aggregateId(aggregateId)
                .aggregateVersion(aggregateVersion)
                .eventId(UUID.randomUUID().toString())
                .eventType(eventType)
                .producer(PRODUCER)
                .schemaVersion(EVENT_SCHEMA_VERSION)
                .payload(payload)
                .occurredAt(now)
                .status(OutboxEventStatus.PENDING)
                .retryCount(0)
                .build();

        event.setCreatedAt(now);
        event.setUpdatedAt(now);

        return outboxEventRepository.save(event);
    }
}