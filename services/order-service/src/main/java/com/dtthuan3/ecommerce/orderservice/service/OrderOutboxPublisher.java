package com.dtthuan3.ecommerce.orderservice.service;

import com.dtthuan3.ecommerce.orderservice.entity.OrderOutbox;
import com.dtthuan3.ecommerce.orderservice.repository.OrderOutboxRepository;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Day outbox -> Kafka at-least-once (consumer dedup theo eventId). Chay moi 5s. */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderOutboxPublisher {

    private final OrderOutboxRepository outbox;
    private final KafkaTemplate<String, String> kafka;

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void publishPending() {
        for (OrderOutbox o : outbox.findTop50ByPublishedAtIsNullOrderByCreatedAtAsc()) {
            try {
                kafka.send(o.getTopic(), o.getAggregateId(), o.getPayload()).get(5, TimeUnit.SECONDS);
                o.setPublishedAt(java.time.Instant.now());
            } catch (Exception e) {
                o.setAttempts(o.getAttempts() + 1);
                log.warn("Publish outbox event {} failed (attempt {}): {}",
                        o.getEventId(), o.getAttempts(), e.getMessage());
            }
            outbox.save(o);
        }
    }
}
