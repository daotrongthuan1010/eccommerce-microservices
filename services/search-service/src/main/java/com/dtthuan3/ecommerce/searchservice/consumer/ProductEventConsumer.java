package com.dtthuan3.ecommerce.searchservice.consumer;

import com.dtthuan3.ecommerce.searchservice.client.ProductServiceClient;
import com.dtthuan3.ecommerce.searchservice.dto.ProductDetailDto;
import com.dtthuan3.ecommerce.searchservice.index.ProductDocument;
import com.dtthuan3.ecommerce.searchservice.service.ProductIndexService;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Consumer topic product-events (outbox cua product-service → Kafka relay).
 * Event chi mang ID (ProductPublished {productId, revisionId}) — enrich
 * bang cach goi lai product-service roi index document day du vao ES.
 *
 * §7 idempotency: index la upsert theo productId (consume lai khong tao
 * ban ghi kep) + dedup eventId trong memory (restart mat dedup-set nhung
 * upsert lam cho viec nay an toan — at-least-once + idempotent handler).
 * Poison pill (productId khong phai UUID...) → ack bo qua + log, khong de
 * DefaultErrorHandler retry vo han block partition.
 */
@Component
@KafkaListener(
        topics = "product-events",
        groupId = "search-service",
        concurrency = "2",
        containerFactory = "searchKafkaListenerContainerFactory")
public class ProductEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(ProductEventConsumer.class);

    /** Dedup eventId gan nhat (muc de phong duplicate event tu outbox retry). */
    private static final int DEDUP_MAX = 10_000;

    private final ProductServiceClient productService;
    private final ProductIndexService indexService;
    private final Map<String, Boolean> seenEventIds = new ConcurrentHashMap<>();

    public ProductEventConsumer(ProductServiceClient productService, ProductIndexService indexService) {
        this.productService = productService;
        this.indexService = indexService;
    }

    /**
     * 1 handler duy nhat cho moi payload (LinkedHashMap tu JsonDeserializer).
     * Record deserialize fail (test cu, tombstone) ve null → handle null an
     * toan roi ack. KHONG tao them @KafkaHandler(Object) — spring-kafka bao
     * "Ambiguous methods" khi 2 handler cung match LinkedHashMap.
     */
    @KafkaHandler
    public void onProductEvent(Map<String, Object> event, Acknowledgment ack) {
        try {
            if (event == null || event.isEmpty()) {
                // Record hong (ErrorHandlingDeserializer tra null) — ack bo qua.
                log.warn("Nhan record khong parse duoc (null/empty) — ack bo qua");
                return;
            }
            handle(event);
        } catch (Exception e) {
            // Index ES fail (ES down...): log + ack — khong block partition;
            // reindex admin se vay lai toan bo khi ES quay lai.
            log.error("Xu ly event that bai {}: {}", event.get("eventId"), e.getMessage(), e);
        } finally {
            ack.acknowledge();
        }
    }

    private void handle(Map<String, Object> event) {
        String eventType = String.valueOf(event.get("eventType"));
        String eventId = String.valueOf(event.get("eventId"));
        String productIdRaw = String.valueOf(event.get("productId"));

        if (!"ProductPublished".equals(eventType)) {
            log.debug("Bo qua event type {}", eventType);
            return;
        }

        // Dedup eventId (outbox retry co the gui lai cung event).
        if (eventId != null && seenEventIds.putIfAbsent(eventId, Boolean.TRUE) != null) {
            log.info("Event {} da xu ly — bo duplicate", eventId);
            return;
        }
        if (seenEventIds.size() > DEDUP_MAX) {
            seenEventIds.clear(); // tranh memory leak; upsert dam bao idempotent
        }

        UUID productId;
        try {
            productId = UUID.fromString(productIdRaw);
        } catch (IllegalArgumentException e) {
            log.warn("productId khong hop le '{}' — bo qua event {}", productIdRaw, eventId);
            return;
        }

        ProductDetailDto detail = productService.getProduct(productId);
        if (detail == null) {
            // Product khong tra duoc (vua xoa / product-service lag) — ack,
            // reindex toan bo se sua lai sau.
            log.warn("Khong enrich duoc product {} (event {}) — bo qua", productIdRaw, eventId);
            return;
        }

        indexService.index(detail);
        log.info("Da index product {} vao {} (event {})", productIdRaw, ProductDocument.INDEX, eventId);
    }
}
