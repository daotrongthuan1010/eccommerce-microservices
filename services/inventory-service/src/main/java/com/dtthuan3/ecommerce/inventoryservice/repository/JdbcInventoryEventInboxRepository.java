package com.dtthuan3.ecommerce.inventoryservice.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** PostgreSQL adapter dùng unique key và ON CONFLICT để khử trùng lặp an toàn giữa nhiều instance. */
@Repository
@RequiredArgsConstructor
public class JdbcInventoryEventInboxRepository implements InventoryEventInboxRepository {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public InboxClaim claim(String consumerName, String eventId, String payloadHash, Instant processedAt) {
        int inserted = jdbcTemplate.update("""
                        INSERT INTO inventory_event_inbox (
                            id, consumer_name, event_id, payload_hash, processed_at
                        ) VALUES (?, ?, ?, ?, ?)
                        ON CONFLICT (consumer_name, event_id) DO NOTHING
                        """,
                UUID.randomUUID().toString(), consumerName, eventId, payloadHash,
                Timestamp.from(processedAt));
        if (inserted == 1) return new InboxClaim(true, payloadHash);

        List<String> hashes = jdbcTemplate.query("""
                        SELECT payload_hash FROM inventory_event_inbox
                        WHERE consumer_name = ? AND event_id = ?
                        """,
                (resultSet, rowNumber) -> resultSet.getString("payload_hash"), consumerName, eventId);
        if (hashes.isEmpty()) {
            throw new IllegalStateException("Inbox event biến mất trong lúc kiểm tra eventId=" + eventId);
        }
        return new InboxClaim(false, hashes.getFirst());
    }

    @Override
    public int deleteProcessedBefore(Instant cutoff) {
        return jdbcTemplate.update("DELETE FROM inventory_event_inbox WHERE processed_at < ?",
                Timestamp.from(cutoff));
    }
}
