package com.dtthuan3.ecommerce.catalogservice.kafka.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductEvent {

    private UUID eventId;

    private String eventType;

    private String productId;

    private Long eventVersion;

    private Instant occurredAt;

    private ProductEventData data;
}
