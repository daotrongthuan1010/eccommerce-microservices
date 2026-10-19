package com.dtthuan3.ecommerce.productservice.service.impl;

import com.dtthuan3.ecommerce.productservice.constant.OutboxStatus;
import com.dtthuan3.ecommerce.productservice.repository.OutboxEventRepository;
import com.dtthuan3.ecommerce.productservice.service.OutboxPublisherSchedulerService;
import com.dtthuan3.ecommerce.productservice.service.OutboxPublisherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisherSchedulerServiceImpl implements OutboxPublisherSchedulerService {

    private static final int BATCH_SIZE = 100;

    private final OutboxEventRepository outboxEventRepository;
    private final OutboxPublisherService outboxPublisherService;

    @Override
    @Scheduled(fixedDelayString = "${app.outbox.poll-interval-ms:1000}")
    public void publishPendingEvents() {
        List<UUID> eventIds =
                outboxEventRepository.findPendingEventIds(
                        OutboxStatus.PENDING,
                        LocalDateTime.now(),
                        PageRequest.of(0, BATCH_SIZE)
                );

        if (eventIds.isEmpty()) {
            return;
        }

        log.debug("Found {} pending outbox events", eventIds.size());

        for (UUID eventId : eventIds) {
            try {
                outboxPublisherService.publish(eventId);
            } catch (Exception exception) {
                log.error(
                        "Unexpected error processing outbox event: {}",
                        eventId,
                        exception
                );
            }
        }
    }
}

