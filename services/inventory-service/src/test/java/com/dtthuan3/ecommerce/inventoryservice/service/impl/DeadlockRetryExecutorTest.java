package com.dtthuan3.ecommerce.inventoryservice.service.impl;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.sql.SQLException;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Kiểm tra retry chỉ chạy lại deadlock PostgreSQL và không lặp lỗi nghiệp vụ khác. */
class DeadlockRetryExecutorTest {

    @Test
    void retriesDeadlockInANewTransactionAndReturnsSuccessfulAttempt() {
        PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
        when(transactionManager.getTransaction(any(TransactionDefinition.class)))
                .thenReturn(new SimpleTransactionStatus(), new SimpleTransactionStatus());
        doNothing().when(transactionManager).commit(any());
        doNothing().when(transactionManager).rollback(any());
        DeadlockRetryExecutor retryExecutor = new DeadlockRetryExecutor(
                transactionManager, 3, Duration.ofMillis(40), Duration.ofSeconds(1));
        AtomicInteger attempts = new AtomicInteger();

        String result = retryExecutor.execute("test-reservation", () -> {
            if (attempts.incrementAndGet() == 1) {
                throw new IllegalStateException("deadlock", new SQLException("deadlock detected", "40P01"));
            }
            return "success";
        });

        assertEquals("success", result);
        assertEquals(2, attempts.get());
    }

    @Test
    void doesNotRetryNonTransientDatabaseError() {
        PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
        when(transactionManager.getTransaction(any(TransactionDefinition.class)))
                .thenReturn(new SimpleTransactionStatus());
        doNothing().when(transactionManager).commit(any());
        doNothing().when(transactionManager).rollback(any());
        DeadlockRetryExecutor retryExecutor = new DeadlockRetryExecutor(
                transactionManager, 3, Duration.ofMillis(40), Duration.ofSeconds(1));
        AtomicInteger attempts = new AtomicInteger();

        assertThrows(IllegalArgumentException.class, () -> retryExecutor.execute("test-validation", () -> {
            attempts.incrementAndGet();
            throw new IllegalArgumentException("invalid business request",
                    new SQLException("constraint violation", "23514"));
        }));

        assertEquals(1, attempts.get());
    }
}
