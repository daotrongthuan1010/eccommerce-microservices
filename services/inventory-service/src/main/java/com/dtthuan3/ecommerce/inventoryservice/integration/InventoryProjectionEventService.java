package com.dtthuan3.ecommerce.inventoryservice.integration;

import com.dtthuan3.ecommerce.inventoryservice.entity.SkuCatalogProjection;
import com.dtthuan3.ecommerce.inventoryservice.integration.event.ProductSkuEvent;
import com.dtthuan3.ecommerce.inventoryservice.repository.InventoryEventInboxRepository;
import com.dtthuan3.ecommerce.inventoryservice.repository.SkuCatalogProjectionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;

/**
 * Áp dụng Product event atomically cùng dấu inbox để Kafka redelivery không lặp cập nhật projection.
 * Class này nối use case Inventory với hệ thống hoặc luồng sự kiện bên ngoài; nó xử lý chuyển đổi/giao tiếp
 * tại biên tích hợp, còn trạng thái tồn và quy tắc nghiệp vụ vẫn do service Inventory quản lý.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryProjectionEventService {
    private static final String CONSUMER_NAME = "inventory-product-sku-projection-v1";

    private final SkuCatalogProjectionRepository skuRepository;
    private final InventoryEventInboxRepository inboxRepository;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    @Value("${inventory.kafka.inbox-retention}")
    private Duration inboxRetention;

    @Value("${inventory.kafka.inbox-cleanup-batch-size}")
    private int inboxCleanupBatchSize;

    @Value("${inventory.kafka.inbox-cleanup-max-batches}")
    private int inboxCleanupMaxBatches;

    @Value("${inventory.kafka.inbox-cleanup-batch-limit}")
    private int inboxCleanupBatchLimit;

    @Value("${inventory.kafka.inbox-cleanup-max-batches-per-run}")
    private int inboxCleanupMaxBatchesPerRun;

    @PostConstruct
    void validateCleanupConfiguration() {
        if (inboxCleanupBatchSize < 1 || inboxCleanupMaxBatches < 1
                || inboxCleanupBatchLimit < 1 || inboxCleanupMaxBatchesPerRun < 1) {
            throw new IllegalStateException("Cấu hình dọn Inventory inbox phải lớn hơn 0");
        }
    }

    /** Đăng ký event trước trong transaction; projection lỗi sẽ rollback cả dấu inbox. */
    @Transactional
    public ApplyOutcome applyProductEvent(ProductSkuEvent event) {
        validateEnvelope(event);
        // Hash giúp phân biệt redelivery cùng nội dung (an toàn) với eventId bị tái sử dụng cho nội dung khác.
        String payloadHash = calculatePayloadHash(event);
        InventoryEventInboxRepository.InboxClaim claim = inboxRepository.claim(
                CONSUMER_NAME, event.eventId(), payloadHash, Instant.now());

        if (!claim.inserted()) {
            if (!payloadHash.equals(claim.existingPayloadHash())) {
                // Không âm thầm bỏ qua xung đột ID: đó là vi phạm hợp đồng và cần cảnh báo để điều tra producer.
                meterRegistry.counter("inventory.kafka.product_events.id_conflict").increment();
                throw new IllegalStateException("eventId đã được dùng với payload khác: " + event.eventId());
            }
            // Inbox và projection cùng transaction nên event đã claim trước đây cũng đã áp dụng hoặc transaction
            // rollback.
            meterRegistry.counter("inventory.kafka.product_events.duplicates").increment();
            return ApplyOutcome.DUPLICATE;
        }

        boolean active = switch (event.eventType()) {
            case "SkuCreated" -> true;
            case "SkuDisabled" -> false;
            default -> throw new IllegalArgumentException("Không hỗ trợ Product event: " + event.eventType());
        };

        String skuCode = event.skuCode().trim();
        SkuCatalogProjection current = skuRepository.findById(skuCode).orElse(null);
        // Version do Product phát là thứ tự chuẩn; chỉ dùng occurredAt khi một trong hai event thiếu version.
        // So sánh <= cũng bỏ event replay cùng version, tránh event cũ ghi đè trạng thái mới hơn.
        if (isOlder(event.version(), event.occurredAt(),
                current == null ? null : current.getSourceVersion(),
                current == null ? null : current.getSourceUpdatedAt())) {
            incrementAfterCommit("inventory.kafka.product_events.stale", 1);
            return ApplyOutcome.STALE;
        }

        Instant sourceUpdatedAt = event.occurredAt() == null ? Instant.now() : event.occurredAt();
        // Nếu lưu projection lỗi, dấu inbox cũng rollback để Kafka retry có thể thử lại đầy đủ.
        skuRepository.save(new SkuCatalogProjection(skuCode, active, event.version(), sourceUpdatedAt));
        incrementAfterCommit("inventory.kafka.product_events.applied", 1, "event_type", event.eventType());
        return ApplyOutcome.APPLIED;
    }

    /** Dọn inbox theo retention cấu hình để bảng chống trùng không tăng vô hạn. */
    @Scheduled(fixedDelayString = "${inventory.kafka.inbox-cleanup-delay-ms}")
    public void cleanupInbox() {
        try {
            Instant cutoff = Instant.now().minus(inboxRetention);
            int safeBatchSize = Math.max(1, Math.min(inboxCleanupBatchSize, inboxCleanupBatchLimit));
            int safeMaxBatches = Math.max(1, Math.min(inboxCleanupMaxBatches, inboxCleanupMaxBatchesPerRun));
            int deleted = 0;
            // Không bọc cả đợt dọn trong một transaction; mỗi câu SQL kết thúc riêng để nhả lock/WAL sớm.
            for (int batch = 0; batch < safeMaxBatches; batch++) {
                int deletedInBatch = inboxRepository.deleteProcessedBefore(cutoff, safeBatchSize);
                deleted += deletedInBatch;
                if (deletedInBatch < safeBatchSize) break;
            }
            if (deleted > 0) {
                meterRegistry.counter("inventory.kafka.inbox.cleanup.deleted").increment(deleted);
                log.info("Đã xóa {} inbox record quá retention {} theo các lô tối đa {} dòng",
                        deleted, inboxRetention, safeBatchSize);
            }
        } catch (RuntimeException failure) {
            meterRegistry.counter("inventory.kafka.inbox.cleanup.failures").increment();
            log.error("Không dọn được Inventory event inbox", failure);
        }
    }

    private void validateEnvelope(ProductSkuEvent event) {
        if (event == null || event.eventId() == null || event.eventId().isBlank()
                || event.eventId().length() > 200 || event.eventType() == null
                || event.skuCode() == null || event.skuCode().isBlank()) {
            throw new IllegalArgumentException("Product event cần eventId (tối đa 200 ký tự), eventType và skuCode");
        }
        if (event.version() != null && event.version() < 0) {
            throw new IllegalArgumentException("version của Product event không được âm");
        }
    }

    private String calculatePayloadHash(ProductSkuEvent event) {
        try {
            byte[] canonicalPayload = objectMapper.writeValueAsBytes(event);
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(canonicalPayload);
            return HexFormat.of().formatHex(hash);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Không thể chuẩn hóa Product event để kiểm tra trùng", exception);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("JVM không hỗ trợ SHA-256", exception);
        }
    }

    /** Tránh ghi metric thành công trước khi inbox và projection cùng commit. */
    private void incrementAfterCommit(String metricName, double amount, String... tags) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                meterRegistry.counter(metricName, tags).increment(amount);
            }
        });
    }

    private boolean isOlder(Long incomingVersion, Instant incomingTime, Long currentVersion, Instant currentTime) {
        if (incomingVersion != null && currentVersion != null) {
            return incomingVersion <= currentVersion;
        }
        // Khi thiếu version, dùng thời gian nguồn để loại event đến muộn.
        return incomingTime != null && currentTime != null && incomingTime.isBefore(currentTime);
    }

    /**
     * Kết quả áp dụng event vào projection: mới được ghi, cũ hơn bị bỏ qua hoặc bị trùng.
     * Enum này mô tả kết quả xử lý tích hợp để luồng gọi biết event được áp dụng, bỏ qua do cũ hay nhận diện là
     * trùng.
     */
    public enum ApplyOutcome {
        APPLIED,
        STALE,
        DUPLICATE
    }
}
