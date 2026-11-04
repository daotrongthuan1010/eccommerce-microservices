package com.dtthuan3.ecommerce.inventoryservice.repository.customrepo;

import com.dtthuan3.ecommerce.inventoryservice.repository.InventoryEventInboxRepository.InboxClaim;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Kiểm tra PostgreSQL chỉ cho một consumer claim cùng event khi nhiều replica nhận bản gửi lặp. */
@Testcontainers
class JdbcInventoryEventInboxRepositoryConcurrencyTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    private JdbcTemplate jdbcTemplate;
    private JdbcInventoryEventInboxRepository repository;

    @BeforeEach
    void createInboxFixture() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        dataSource.setDriverClassName("org.postgresql.Driver");
        jdbcTemplate = new JdbcTemplate(dataSource);
        repository = new JdbcInventoryEventInboxRepository(jdbcTemplate);
        jdbcTemplate.execute("DROP TABLE IF EXISTS inventory_event_inbox");
        jdbcTemplate.execute("""
                CREATE TABLE inventory_event_inbox (
                    id VARCHAR(36) PRIMARY KEY,
                    consumer_name VARCHAR(120) NOT NULL,
                    event_id VARCHAR(200) NOT NULL,
                    payload_hash VARCHAR(64) NOT NULL,
                    processed_at TIMESTAMPTZ NOT NULL,
                    CONSTRAINT uk_inventory_inbox_consumer_event UNIQUE (consumer_name, event_id)
                )
                """);
    }

    @AfterEach
    void dropInboxFixture() throws Exception {
        try (Connection connection = jdbcTemplate.getDataSource().getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE IF EXISTS inventory_event_inbox");
        }
    }

    @Test
    void concurrentRedeliveryCanOnlyBeClaimedOnce() throws Exception {
        int deliveryCount = 30;
        ExecutorService executor = Executors.newFixedThreadPool(deliveryCount);
        CountDownLatch ready = new CountDownLatch(deliveryCount);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<InboxClaim>> results = new ArrayList<>();

        try {
            for (int index = 0; index < deliveryCount; index++) {
                results.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    return repository.claim("return-approval", "event-1", "a".repeat(64), Instant.now());
                }));
            }

            ready.await();
            start.countDown();
            int winners = 0;
            for (Future<InboxClaim> result : results) {
                if (result.get().inserted()) winners++;
            }

            assertEquals(1, winners);
            assertEquals(1, jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM inventory_event_inbox WHERE consumer_name = 'return-approval' AND event_id = 'event-1'",
                    Integer.class));
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void reusedEventIdWithDifferentPayloadReturnsOriginalHash() {
        InboxClaim first = repository.claim("product-projection", "event-2", "b".repeat(64), Instant.now());
        InboxClaim duplicate = repository.claim("product-projection", "event-2", "c".repeat(64), Instant.now());

        assertTrue(first.inserted());
        assertFalse(duplicate.inserted());
        assertEquals("b".repeat(64), duplicate.existingPayloadHash());
    }
}
