package com.dtthuan3.ecommerce.inventoryservice.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** PostgreSQL adapter: batch insert, leasing query và cập nhật retry/publish bằng SQL rõ ràng. */
@Repository
@RequiredArgsConstructor
public class JdbcInventoryEventOutboxRepository implements InventoryEventOutboxRepository {
    private static final String INSERT_SQL = """
            INSERT INTO inventory_event_outbox (
                event_id, topic, event_type, aggregate_id, payload,
                created_at, available_at, attempts, published_at, last_error
            ) VALUES (?, ?, ?, ?, ?, clock_timestamp(), clock_timestamp(), 0, NULL, NULL)
            """;

    private static final RowMapper<OutboxMessage> MESSAGE_ROW_MAPPER = (resultSet, rowNumber) ->
            new OutboxMessage(resultSet.getString("event_id"), resultSet.getString("topic"),
                    resultSet.getString("event_type"), resultSet.getString("aggregate_id"),
                    resultSet.getString("payload"), resultSet.getInt("attempts"));

    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void appendBatch(List<OutboxMessage> messages) {
        if (messages == null || messages.isEmpty()) return;
        jdbcTemplate.batchUpdate(INSERT_SQL, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement statement, int index) throws SQLException {
                OutboxMessage message = messages.get(index);
                statement.setString(1, message.eventId());
                statement.setString(2, message.topic());
                statement.setString(3, message.eventType());
                statement.setString(4, message.aggregateId());
                statement.setString(5, message.payload());
            }

            @Override
            public int getBatchSize() {
                return messages.size();
            }
        });
    }

    @Override
    @Transactional
    public List<OutboxMessage> claimAvailable(int limit, Instant now, Instant leaseUntil) {
        if (limit < 1) return List.of();
        List<OutboxMessage> claimed = jdbcTemplate.query("""
                        SELECT current_event.event_id, current_event.topic, current_event.event_type,
                               current_event.aggregate_id, current_event.payload, current_event.attempts,
                               current_event.created_at
                        FROM inventory_event_outbox current_event
                        WHERE current_event.published_at IS NULL AND current_event.available_at <= ?
                          AND NOT EXISTS (
                              SELECT 1 FROM inventory_event_outbox earlier_event
                              WHERE earlier_event.aggregate_id = current_event.aggregate_id
                                AND earlier_event.published_at IS NULL
                                AND (earlier_event.created_at < current_event.created_at
                                  OR (earlier_event.created_at = current_event.created_at
                                      AND earlier_event.event_id < current_event.event_id))
                          )
                        ORDER BY current_event.created_at, current_event.event_id
                        LIMIT ? FOR UPDATE SKIP LOCKED
                        """,
                MESSAGE_ROW_MAPPER, Timestamp.from(now), limit);
        if (claimed.isEmpty()) return claimed;

        List<OutboxMessage> leased = new ArrayList<>(claimed.size());
        jdbcTemplate.batchUpdate("""
                        UPDATE inventory_event_outbox
                        SET available_at = ?, attempts = attempts + 1
                        WHERE event_id = ? AND published_at IS NULL
                        """,
                new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement statement, int index) throws SQLException {
                        statement.setTimestamp(1, Timestamp.from(leaseUntil));
                        statement.setString(2, claimed.get(index).eventId());
                    }

                    @Override
                    public int getBatchSize() {
                        return claimed.size();
                    }
                });
        for (OutboxMessage message : claimed) {
            leased.add(new OutboxMessage(message.eventId(), message.topic(), message.eventType(),
                    message.aggregateId(), message.payload(), message.attempts() + 1));
        }
        return leased;
    }

    @Override
    public boolean markPublished(String eventId, Instant publishedAt) {
        return jdbcTemplate.update("""
                        UPDATE inventory_event_outbox
                        SET published_at = ?, last_error = NULL
                        WHERE event_id = ? AND published_at IS NULL
                        """,
                Timestamp.from(publishedAt), eventId) == 1;
    }

    @Override
    public boolean scheduleRetry(String eventId, Instant availableAt, String errorMessage) {
        String safeError = errorMessage == null ? "Unknown Kafka publish error"
                : errorMessage.substring(0, Math.min(errorMessage.length(), 1000));
        return jdbcTemplate.update("""
                        UPDATE inventory_event_outbox
                        SET available_at = ?, last_error = ?
                        WHERE event_id = ? AND published_at IS NULL
                        """,
                Timestamp.from(availableAt), safeError, eventId) == 1;
    }

    @Override
    public long countPending() {
        Long pending = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM inventory_event_outbox WHERE published_at IS NULL", Long.class);
        return pending == null ? 0 : pending;
    }

    @Override
    public Optional<Instant> oldestPendingCreatedAt() {
        List<Timestamp> timestamps = jdbcTemplate.query("""
                        SELECT MIN(created_at) AS oldest_created_at
                        FROM inventory_event_outbox WHERE published_at IS NULL
                        """,
                (resultSet, rowNumber) -> resultSet.getTimestamp("oldest_created_at"));
        Timestamp oldest = timestamps.isEmpty() ? null : timestamps.getFirst();
        return Optional.ofNullable(oldest).map(Timestamp::toInstant);
    }

    @Override
    public int deletePublishedBefore(Instant cutoff) {
        return jdbcTemplate.update("""
                        DELETE FROM inventory_event_outbox
                        WHERE published_at IS NOT NULL AND published_at < ?
                        """,
                Timestamp.from(cutoff));
    }
}
