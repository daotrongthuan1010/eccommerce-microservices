package com.dtthuan3.ecommerce.inventoryservice.repository.customrepo;

import com.dtthuan3.ecommerce.inventoryservice.repository.InventoryStockOperationRepository.StockOperationRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Kiểm tra unique operation key ở PostgreSQL khi hai request nhập/xuất cùng được xử lý đồng thời. */
@Testcontainers
class JdbcInventoryStockOperationRepositoryConcurrencyTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    private JdbcTemplate jdbcTemplate;
    private JdbcInventoryStockOperationRepository repository;
    private TransactionTemplate transactions;

    @BeforeEach
    void createOperationFixture() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        dataSource.setDriverClassName("org.postgresql.Driver");
        jdbcTemplate = new JdbcTemplate(dataSource);
        repository = new JdbcInventoryStockOperationRepository(jdbcTemplate);
        transactions = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
        jdbcTemplate.execute("DROP TABLE IF EXISTS inventory_stock_operation");
        jdbcTemplate.execute("""
                CREATE TABLE inventory_stock_operation (
                    id VARCHAR(36) PRIMARY KEY,
                    operation_id VARCHAR(120) NOT NULL UNIQUE,
                    operation_type VARCHAR(40) NOT NULL,
                    inventory_id VARCHAR(36),
                    payload_hash VARCHAR(64) NOT NULL,
                    response_json TEXT,
                    created_at TIMESTAMP NOT NULL
                )
                """);
    }

    @Test
    void concurrentRetryClaimsOnlyOnceAndReadsCommittedResponse() throws Exception {
        CountDownLatch firstClaimed = new CountDownLatch(1);
        CountDownLatch releaseFirstTransaction = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> first = executor.submit(() -> transactions.execute(status -> {
                boolean claimed = repository.tryStart("receive-key-1", "RECEIVE_EXISTING", "inventory-1",
                        "a".repeat(64), LocalDateTime.now());
                firstClaimed.countDown();
                await(releaseFirstTransaction);
                repository.complete("receive-key-1", "inventory-1", "{\"id\":\"inventory-1\"}");
                return claimed;
            }));

            firstClaimed.await();
            Future<StockOperationRecord> retry = executor.submit(() -> transactions.execute(status -> {
                boolean claimed = repository.tryStart("receive-key-1", "RECEIVE_EXISTING", "inventory-1",
                        "a".repeat(64), LocalDateTime.now());
                assertFalse(claimed, "Retry không được claim khóa đã có request đầu tiên");
                return repository.findByOperationId("receive-key-1").orElseThrow();
            }));

            releaseFirstTransaction.countDown();
            assertTrue(first.get());
            StockOperationRecord record = retry.get();
            assertEquals("RECEIVE_EXISTING", record.operationType());
            assertEquals("a".repeat(64), record.payloadHash());
            assertEquals("{\"id\":\"inventory-1\"}", record.responseJson());
            assertEquals(1, jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM inventory_stock_operation WHERE operation_id = 'receive-key-1'", Integer.class));
        } finally {
            releaseFirstTransaction.countDown();
            executor.shutdownNow();
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Test bị ngắt khi chờ transaction", exception);
        }
    }
}
