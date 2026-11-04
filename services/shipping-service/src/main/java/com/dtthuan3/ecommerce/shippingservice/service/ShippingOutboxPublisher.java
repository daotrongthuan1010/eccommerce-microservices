package com.dtthuan3.ecommerce.shippingservice.service;

import com.dtthuan3.ecommerce.shippingservice.entity.ShippingOutbox;
import com.dtthuan3.ecommerce.shippingservice.repository.ShippingOutboxRepository;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Day outbox -> Kafka (at-least-once, consumer dedup theo eventId). Chay moi 5s. */
@Component
public class ShippingOutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(ShippingOutboxPublisher.class);
    private final ShippingOutboxRepository outbox;
    private final KafkaTemplate<String, String> kafka;

    public ShippingOutboxPublisher(ShippingOutboxRepository outbox, KafkaTemplate<String, String> kafka) {
        this.outbox = outbox;
        this.kafka = kafka;
    }

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void publishPending() {
        for (ShippingOutbox o : outbox.findTop50ByPublishedAtIsNullOrderByCreatedAtAsc()) {
            try {
                kafka.send(o.getTopic(), o.getAggregateId(), o.getPayload()).get(5, TimeUnit.SECONDS);
                o.setPublishedAt(Instant.now());
            } catch (Exception e) {
                o.setAttempts(o.getAttempts() + 1);
                log.warn("Publish outbox event {} failed (attempt {}): {}", o.getEventId(), o.getAttempts(), e.getMessage());
            }
            outbox.save(o);
        }
    }
}
