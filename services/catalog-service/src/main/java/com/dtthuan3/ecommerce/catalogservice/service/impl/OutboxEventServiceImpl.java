package com.dtthuan3.ecommerce.catalogservice.service.impl;

import com.dtthuan3.ecommerce.catalogservice.contstant.OutboxEventStatus;
import com.dtthuan3.ecommerce.catalogservice.entity.OutboxEvent;
import com.dtthuan3.ecommerce.catalogservice.repository.OutboxEventRepository;
import com.dtthuan3.ecommerce.catalogservice.service.OutboxEventService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OutboxEventServiceImpl
        implements OutboxEventService {

    private final OutboxEventRepository outboxEventRepository;

    @Override
    public OutboxEvent createEvent(
            String aggregateType,
            Long aggregateId,
            String eventType,
            String payload
    ) {

        OutboxEvent event =
                OutboxEvent.builder()
                        .aggregateType(aggregateType)
                        .aggregateId(aggregateId)
                        .eventId(UUID.randomUUID())
                        .eventType(eventType)
                        .producer("catalog-service")
                        .schemaVersion(1)
                        .payload(payload)
                        .occurredAt(LocalDateTime.now())
                        .status(
                                OutboxEventStatus.PENDING
                        )
                        .retryCount(0)
                        .build();
        event.setCreatedAt(LocalDateTime.now());
        event.setUpdatedAt(LocalDateTime.now());

        return outboxEventRepository.save(event);
    }
}