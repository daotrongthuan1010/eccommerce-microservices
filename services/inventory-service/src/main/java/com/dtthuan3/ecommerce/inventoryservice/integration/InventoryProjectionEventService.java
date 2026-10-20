package com.dtthuan3.ecommerce.inventoryservice.integration;

import com.dtthuan3.ecommerce.inventoryservice.entity.SkuCatalogProjection;
import com.dtthuan3.ecommerce.inventoryservice.integration.event.ProductSkuEvent;
import com.dtthuan3.ecommerce.inventoryservice.repository.InventoryEventInboxRepository;
import com.dtthuan3.ecommerce.inventoryservice.repository.SkuCatalogProjectionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
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

/** Áp dụng Product event atomically cùng dấu inbox để Kafka redelivery không lặp cập nhật projection. */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryProjectionEventService {
    private static final String CONSUMER_NAME = "inventory-product-sku-projection-v1";

    private final SkuCatalogProjectionRepository skuRepository;
    private final InventoryEventInboxRepository inboxRepository;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    @Value("${inventory.kafka.inbox-retention:PT90D}")
    private Duration inboxRetention;

    /** Đăng ký event trước trong transaction; projection lỗi sẽ rollback cả dấu inbox. */
    @Transactional
    public ApplyOutcome applyProductEvent(ProductSkuEvent event) {
        validateEnvelope(event);
        String payloadHash = calculatePayloadHash(event);
        InventoryEventInboxRepository.InboxClaim claim = inboxRepository.claim(
                CONSUMER_NAME, event.eventId(), payloadHash, Instant.now());

        if (!claim.inserted()) {
            if (!payloadHash.equals(claim.existingPayloadHash())) {
                meterRegistry.counter("inventory.kafka.product_events.id_conflict").increment();
                throw new IllegalStateException("eventId đã được dùng với payload khác: " + event.eventId());
            }
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
        // Version từ nguồn là thứ tự chính; occurredAt chỉ làm dự phòng khi thiếu version.
        if (isOlder(event.version(), event.occurredAt(),
                current == null ? null : current.getSourceVersion(),
                current == null ? null : current.getSourceUpdatedAt())) {
            incrementAfterCommit("inventory.kafka.product_events.stale", 1);
            return ApplyOutcome.STALE;
        }

        Instant sourceUpdatedAt = event.occurredAt() == null ? Instant.now() : event.occurredAt();
        skuRepository.save(new SkuCatalogProjection(skuCode, active, event.version(), sourceUpdatedAt));
        incrementAfterCommit("inventory.kafka.product_events.applied", 1, "event_type", event.eventType());
        return ApplyOutcome.APPLIED;
    }

    /** Dọn inbox theo retention cấu hình để bảng chống trùng không tăng vô hạn. */
    @Scheduled(fixedDelayString = "${inventory.kafka.inbox-cleanup-delay-ms:86400000}")
    @Transactional
    public void cleanupInbox() {
        int deleted = inboxRepository.deleteProcessedBefore(Instant.now().minus(inboxRetention));
        if (deleted > 0) {
            incrementAfterCommit("inventory.kafka.inbox.cleanup.deleted", deleted);
            log.info("Đã xóa {} inbox record cũ hơn retention {}", deleted, inboxRetention);
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

    public enum ApplyOutcome {
        APPLIED,
        STALE,
        DUPLICATE
    }
}
