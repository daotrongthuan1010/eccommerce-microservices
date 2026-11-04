package com.dtthuan3.ecommerce.orderservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;

/** Inbox dedup: (eventId, source) unique — event Kafka den lai khong tac dong lan 2. */
@Getter
@Setter
@Entity
@Table(name = "order_inbox")
@IdClass(OrderInbox.InboxKey.class)
public class OrderInbox {
    @Id @Column(name = "event_id", length = 36) private String eventId;
    @Id @Column(nullable = false, length = 64) private String source;
    @Column(name = "event_type", nullable = false, length = 64) private String eventType;
    @Column(name = "processed_at", nullable = false) private Instant processedAt;

    /** Composite key (event_id, source). */
    public static class InboxKey implements Serializable {
        private String eventId;
        private String source;

        public InboxKey() {}

        public InboxKey(String eventId, String source) {
            this.eventId = eventId;
            this.source = source;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof InboxKey that)) return false;
            return Objects.equals(eventId, that.eventId) && Objects.equals(source, that.source);
        }

        @Override
        public int hashCode() { return Objects.hash(eventId, source); }
    }
}
