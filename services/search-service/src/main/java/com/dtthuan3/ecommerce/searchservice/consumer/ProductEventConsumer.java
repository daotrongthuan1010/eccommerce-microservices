package com.dtthuan3.ecommerce.searchservice.consumer;

import com.dtthuan3.ecommerce.searchservice.client.ProductServiceClient;
import com.dtthuan3.ecommerce.searchservice.dto.ProductDetailDto;
import com.dtthuan3.ecommerce.searchservice.index.ProductDocument;
import com.dtthuan3.ecommerce.searchservice.service.ProductIndexService;
import java.util.Map;
import java.util.UUID;
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
 * Ack manual (enable-auto-commit=false): chi ack khi index thanh cong;
 * loi → khong ack → poll lai (giai thich docs/vi-search.md §3).
 * JSON payload parse tho qua Map de khong rang buoc schema event
 * (them field moi khong lam consumer hong).
 */
@Component
@KafkaListener(topics = "product-events", groupId = "search-service", concurrency = "2")
public class ProductEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(ProductEventConsumer.class);

    private final ProductServiceClient productService;
    private final ProductIndexService indexService;

    public ProductEventConsumer(ProductServiceClient productService, ProductIndexService indexService) {
        this.productService = productService;
        this.indexService = indexService;
    }

    @KafkaHandler
    public void onProductPublished(Map<String, Object> event, Acknowledgment ack) {
        String eventType = String.valueOf(event.get("eventType"));
        String productId = String.valueOf(event.get("productId"));

        if (!"ProductPublished".equals(eventType)) {
            // Event khac (Rejected, Submitted...) chua lien quan den index search.
            log.debug("Bo qua event type {}", eventType);
            ack.acknowledge();
            return;
        }

        ProductDetailDto detail = productService.getProduct(UUID.fromString(productId));
        if (detail == null) {
            // Product khong tra duoc (vua xoa / product-service lag): van ack de
            // khong block partition; reindex toan bo se sua lai sau.
            log.warn("Khong enrich duoc product {} — bo qua event", productId);
            ack.acknowledge();
            return;
        }

        indexService.index(detail);
        log.info("Da index product {} vao {}", productId, ProductDocument.INDEX);
        ack.acknowledge();
    }

    @KafkaHandler
    public void onUnknown(Object raw, Acknowledgment ack) {
        // Tin nhan khong parse duoc thanh Map (format cu, tombstone...) —
        // ack de khong bi kẹt offset, log canh bao de theo doi.
        log.warn("Nhan tin nhan khong xu ly duoc: {} — ack bo qua", raw);
        ack.acknowledge();
    }
}
