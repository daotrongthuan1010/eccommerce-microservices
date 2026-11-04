package com.dtthuan3.ecommerce.paymentservice.service;

import com.dtthuan3.ecommerce.paymentservice.entity.PaymentOutbox;
import com.dtthuan3.ecommerce.paymentservice.repository.PaymentOutboxRepository;
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
public class PaymentOutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(PaymentOutboxPublisher.class);
    private final PaymentOutboxRepository outbox;
    private final KafkaTemplate<String, String> kafka;

    public PaymentOutboxPublisher(PaymentOutboxRepository outbox, KafkaTemplate<String, String> kafka) {
        this.outbox = outbox;
        this.kafka = kafka;
    }

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void publishPending() {
        for (PaymentOutbox o : outbox.findTop50ByPublishedAtIsNullOrderByCreatedAtAsc()) {
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
