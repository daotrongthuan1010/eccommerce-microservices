package com.dtthuan3.ecommerce.catalogservice.kafka.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record ProductPublishedEvent(
        UUID eventId,
        String eventType,
        Integer eventVersion,
        LocalDateTime occurredAt,
        UUID productId,
        UUID revisionId,
        UUID approvalId,
        UUID actorId
) {
}
