package com.dtthuan3.ecommerce.inventoryservice.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.beans.factory.annotation.Value;

import java.sql.SQLException;
import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

/**
 * Chạy lại toàn bộ use case trong transaction mới khi PostgreSQL báo deadlock hoặc serialization failure.
 * Class này hiện thực use case hoặc tác vụ nền của Inventory, phối hợp kiểm tra nghiệp vụ, repository và
 * tích hợp cần thiết để hoàn tất một bước xử lý; adapter bên ngoài không tự thao tác dữ liệu.
 */
@Slf4j
@Component
public class DeadlockRetryExecutor {
    private final TransactionTemplate transactionTemplate;
    private final int maxAttempts;
    private final long baseBackoffMillis;
    private final long maxBackoffMillis;

    public DeadlockRetryExecutor(PlatformTransactionManager transactionManager,
                                @Value("${inventory.database.deadlock-retry.max-attempts}") int maxAttempts,
                                @Value("${inventory.database.deadlock-retry.base-backoff}") Duration baseBackoff,
                                @Value("${inventory.database.deadlock-retry.max-backoff}") Duration maxBackoff) {
        if (maxAttempts < 1 || baseBackoff.isNegative() || baseBackoff.isZero()
                || maxBackoff.compareTo(baseBackoff) < 0) {
            throw new IllegalArgumentException(
                    "Cấu hình retry deadlock cần attempts >= 1 và max-backoff >= base-backoff > 0");
        }
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.maxAttempts = maxAttempts;
        this.baseBackoffMillis = baseBackoff.toMillis();
        this.maxBackoffMillis = maxBackoff.toMillis();
    }

    /** Mỗi vòng lặp chỉ tiếp tục sau khi transaction trước đã rollback hoàn toàn. */
    public <T> T execute(String operation, Supplier<T> work) {
        // Không lồng retry vào transaction ngoài: PostgreSQL đánh dấu transaction deadlock là aborted và không thể
        // dùng lại.
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("Deadlock retry phải được gọi ngoài transaction hiện tại");
        }

        for (int attempt = 1; ; attempt++) {
            try {
                // Mỗi attempt dùng transaction mới để lock và thay đổi dở của attempt trước đã được giải phóng.
                return transactionTemplate.execute(status -> work.get());
            } catch (RuntimeException exception) {
                if (attempt >= maxAttempts || !isRetryable(exception)) {
                    throw exception;
                }

                // Exponential backoff kèm jitter giảm khả năng các request cạnh tranh thức dậy rồi deadlock lại cùng lúc.
                long multiplier = 1L << Math.min(attempt - 1, 30);
                long exponentialBackoff = baseBackoffMillis > maxBackoffMillis / multiplier
                        ? maxBackoffMillis : baseBackoffMillis * multiplier;
                long jitterLimit = Math.min(baseBackoffMillis, maxBackoffMillis - exponentialBackoff);
                long delay = exponentialBackoff + ThreadLocalRandom.current().nextLong(jitterLimit + 1L);
                log.warn("PostgreSQL deadlock/serialization khi {} (lần {}/{}); thử lại sau {} ms",
                        operation, attempt, maxAttempts, delay);
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
        // Chỉ retry deadlock/serialization failure từ PostgreSQL; lỗi nghiệp vụ hay dữ liệu không được lặp mù
        // quáng.
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
