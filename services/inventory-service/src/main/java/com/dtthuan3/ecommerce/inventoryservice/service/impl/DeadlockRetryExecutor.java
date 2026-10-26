package com.dtthuan3.ecommerce.inventoryservice.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.SQLException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

/** Chạy lại toàn bộ use case trong transaction mới khi PostgreSQL báo deadlock hoặc serialization failure. */
@Slf4j
@Component
public class DeadlockRetryExecutor {
    private static final int MAX_ATTEMPTS = 3;
    private static final int BASE_BACKOFF_MILLIS = 40;

    private final TransactionTemplate transactionTemplate;

    public DeadlockRetryExecutor(PlatformTransactionManager transactionManager) {
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    /** Mỗi vòng lặp chỉ tiếp tục sau khi transaction trước đã rollback hoàn toàn. */
    public <T> T execute(String operation, Supplier<T> work) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("Deadlock retry phải được gọi ngoài transaction hiện tại");
        }

        for (int attempt = 1; ; attempt++) {
            try {
                return transactionTemplate.execute(status -> work.get());
            } catch (RuntimeException exception) {
                if (attempt >= MAX_ATTEMPTS || !isRetryable(exception)) {
                    throw exception;
                }

                long delay = BASE_BACKOFF_MILLIS * (1L << (attempt - 1))
                        + ThreadLocalRandom.current().nextLong(BASE_BACKOFF_MILLIS + 1L);
                log.warn("PostgreSQL deadlock/serialization khi {} (lần {}/{}); thử lại sau {} ms",
                        operation, attempt, MAX_ATTEMPTS, delay);
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    exception.addSuppressed(interrupted);
                    throw exception;
                }
            }
        }
    }

    private boolean isRetryable(Throwable failure) {
        for (Throwable current = failure; current != null; current = current.getCause()) {
            if (current instanceof SQLException sqlException) {
                for (SQLException sql = sqlException; sql != null; sql = sql.getNextException()) {
                    String state = sql.getSQLState();
                    if ("40P01".equals(state) || "40001".equals(state)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
