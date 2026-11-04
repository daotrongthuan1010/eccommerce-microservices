package com.dtthuan3.ecommerce.inventoryservice.repository.customrepo;

import com.dtthuan3.ecommerce.inventoryservice.repository.InventoryEventOutboxRepository.OutboxMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Xác nhận outbox không phát event sau trước event trước của cùng aggregate, kể cả qua nhiều lần poll. */
@Testcontainers
class JdbcInventoryEventOutboxOrderingTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    private JdbcTemplate jdbcTemplate;
    private JdbcInventoryEventOutboxRepository repository;
    private TransactionTemplate transactions;

    @BeforeEach
    void createOutboxFixture() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        dataSource.setDriverClassName("org.postgresql.Driver");
        jdbcTemplate = new JdbcTemplate(dataSource);
        repository = new JdbcInventoryEventOutboxRepository(jdbcTemplate);
        transactions = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
        jdbcTemplate.execute("DROP TABLE IF EXISTS inventory_event_outbox");
        jdbcTemplate.execute("DROP SEQUENCE IF EXISTS inventory_event_outbox_order_seq");
        jdbcTemplate.execute("CREATE SEQUENCE inventory_event_outbox_order_seq");
        jdbcTemplate.execute("""
                CREATE TABLE inventory_event_outbox (
                    event_id VARCHAR(200) PRIMARY KEY,
                    topic VARCHAR(255) NOT NULL,
                    event_type VARCHAR(120) NOT NULL,
                    aggregate_id VARCHAR(200) NOT NULL,
                    payload TEXT NOT NULL,
                    sequence_id BIGINT,
                    created_at TIMESTAMPTZ NOT NULL,
                    available_at TIMESTAMPTZ NOT NULL,
                    attempts INTEGER NOT NULL,
                    published_at TIMESTAMPTZ,
                    last_error TEXT,
                    lease_token VARCHAR(36)
                )
                """);
    }

    @Test
    void eventsForSameAggregateAreClaimedInCreatedOrder() {
        transactions.executeWithoutResult(status -> repository.appendBatch(List.of(
                new OutboxMessage("event-1", "inventory-events", "StockChanged", "inventory-1", "{}", 0, null),
                new OutboxMessage("event-2", "inventory-events", "StockChanged", "inventory-1", "{}", 0, null))));

        Instant now = Instant.now();
        List<OutboxMessage> firstClaim = transactions.execute(status ->
                repository.claimAvailable(10, now, now.plusSeconds(180)));
        assertEquals(1, firstClaim.size());
        assertEquals("event-1", firstClaim.getFirst().eventId());

        OutboxMessage first = firstClaim.getFirst();
        assertTrue(repository.markPublished(first.eventId(), first.leaseToken(), Instant.now()));

        List<OutboxMessage> secondClaim = transactions.execute(status ->
                repository.claimAvailable(10, Instant.now(), Instant.now().plusSeconds(180)));
        assertEquals(1, secondClaim.size());
        assertEquals("event-2", secondClaim.getFirst().eventId());
    }

    @Test
    void outboxInsertRollsBackWithBusinessTransaction() {
        assertThrows(IllegalStateException.class, () -> transactions.executeWithoutResult(status -> {
            repository.appendBatch(List.of(new OutboxMessage(
                    "rolled-back-event", "inventory-events", "StockChanged", "inventory-1", "{}", 0, null)));
            throw new IllegalStateException("Giả lập lỗi sau khi ghi outbox nhưng trước commit nghiệp vụ");
        }));

        assertEquals(0L, repository.countPending(),
                "Không được giữ event outbox nếu transaction nghiệp vụ chứa event bị rollback");
    }
}
