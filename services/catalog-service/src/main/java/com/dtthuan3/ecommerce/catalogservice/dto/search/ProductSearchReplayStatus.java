package com.dtthuan3.ecommerce.catalogservice.dto.search;

import java.time.Instant;
import java.util.UUID;

public record ProductSearchReplayStatus(
        UUID runId,
        String status,
        long processedEvents,
        long failedEvents,
        Instant startedAt,
        Instant finishedAt,
        String lastError
) { }
