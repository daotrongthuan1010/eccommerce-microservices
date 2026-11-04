package com.dtthuan3.ecommerce.inventoryservice.repository.customrepo;

import com.dtthuan3.ecommerce.inventoryservice.mapper.InventoryRowMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Kiểm tra câu UPDATE có điều kiện trên PostgreSQL thật khi nhiều request đồng thời giữ cùng một SKU.
 * Test dùng schema tối giản nhưng gọi trực tiếp adapter JDBC đang được Inventory sử dụng trong production.
 */
@Testcontainers
class JdbcInventoryRepositoryConcurrencyTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    private JdbcTemplate jdbcTemplate;
    private JdbcInventoryRepository repository;

    @BeforeEach
    void createInventoryFixture() throws Exception {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        dataSource.setDriverClassName("org.postgresql.Driver");
        jdbcTemplate = new JdbcTemplate(dataSource);
        repository = new JdbcInventoryRepository(jdbcTemplate, new InventoryRowMapper());

        jdbcTemplate.execute("DROP TABLE IF EXISTS inventory");
        jdbcTemplate.execute("""
                CREATE TABLE inventory (
                    id VARCHAR(36) PRIMARY KEY,
                    enterprise_id VARCHAR(100) NOT NULL,
                    warehouse_id VARCHAR(100) NOT NULL,
                    location_id VARCHAR(100) NOT NULL,
                    sku VARCHAR(100) NOT NULL,
                    physical_quantity NUMERIC(19, 4) NOT NULL,
                    reserved_quantity NUMERIC(19, 4) NOT NULL,
                    defective_quantity NUMERIC(19, 4) NOT NULL,
                    pending_quantity NUMERIC(19, 4) NOT NULL,
                    in_transit_quantity NUMERIC(19, 4) NOT NULL,
                    version BIGINT NOT NULL,
                    created_at TIMESTAMP NOT NULL,
                    updated_at TIMESTAMP NOT NULL
                )
                """);
        jdbcTemplate.update("""
                INSERT INTO inventory (
                    id, enterprise_id, warehouse_id, location_id, sku,
                    physical_quantity, reserved_quantity, defective_quantity,
                    pending_quantity, in_transit_quantity, version, created_at, updated_at
                ) VALUES ('inventory-1', 'enterprise-1', 'warehouse-1', 'location-1', 'SKU-1',
                          10, 0, 0, 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """);
    }

    @AfterEach
    void closeDatabaseConnection() throws Exception {
        // DriverManagerDataSource tạo connection theo yêu cầu; mở connection mới để dọn fixture rồi đóng ngay.
        try (Connection connection = jdbcTemplate.getDataSource().getConnection()) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("DROP TABLE IF EXISTS inventory");
            }
        }
    }

    @Test
    void concurrentReservationsNeverExceedPhysicalQuantity() throws Exception {
        int requestCount = 40;
        ExecutorService executor = Executors.newFixedThreadPool(requestCount);
        CountDownLatch ready = new CountDownLatch(requestCount);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>();

        try {
            for (int index = 0; index < requestCount; index++) {
                results.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    return repository.reserve("inventory-1", BigDecimal.ONE).isPresent();
                }));
            }

            ready.await();
            start.countDown();
            int successfulReservations = 0;
            for (Future<Boolean> result : results) {
                if (result.get()) successfulReservations++;
            }

            assertEquals(10, successfulReservations,
                    "Chỉ 10 request được giữ khi tồn vật lý ban đầu là 10");
            assertEquals(new BigDecimal("10.0000"),
                    jdbcTemplate.queryForObject("SELECT reserved_quantity FROM inventory WHERE id = 'inventory-1'",
                            BigDecimal.class));
            assertEquals(10L, jdbcTemplate.queryForObject(
                    "SELECT version FROM inventory WHERE id = 'inventory-1'", Long.class));
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void reserveRejectsZeroOrInsufficientQuantityWithoutChangingBalance() {
        assertFalse(repository.reserve("inventory-1", BigDecimal.ZERO).isPresent());
        assertFalse(repository.reserve("inventory-1", new BigDecimal("11")).isPresent());
        assertEquals(new BigDecimal("0.0000"), jdbcTemplate.queryForObject(
                "SELECT reserved_quantity FROM inventory WHERE id = 'inventory-1'", BigDecimal.class));
    }
}
