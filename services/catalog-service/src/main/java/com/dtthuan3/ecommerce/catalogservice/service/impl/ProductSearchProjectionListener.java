package com.dtthuan3.ecommerce.catalogservice.service.impl;

import com.dtthuan3.ecommerce.catalogservice.client.ProductServiceClient;
import com.dtthuan3.ecommerce.catalogservice.document.BrandSearchDocument;
import com.dtthuan3.ecommerce.catalogservice.document.CategorySearchDocument;
import com.dtthuan3.ecommerce.catalogservice.document.ManufacturerSearchDocument;
import com.dtthuan3.ecommerce.catalogservice.document.ProductSearchDocument;
import com.dtthuan3.ecommerce.catalogservice.dto.response.ProductSearchSourceResponse;
import com.dtthuan3.ecommerce.catalogservice.repository.BrandsRepository;
import com.dtthuan3.ecommerce.catalogservice.repository.CategoryRepository;
import com.dtthuan3.ecommerce.catalogservice.repository.ManufacturersRepository;
import com.dtthuan3.ecommerce.catalogservice.repository.ProductSearchRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.Locale;

/** Projects published product snapshots into the Catalog-owned search index. */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductSearchProjectionListener {

    private static final Set<String> DELETE_EVENT_TYPES = Set.of(
            "PRODUCT_DELETED", "PRODUCT_SUSPENDED", "PRODUCT_UNPUBLISHED", "PRODUCT_DEACTIVATED");
    private static final Set<String> UPSERT_EVENT_TYPES = Set.of(
            "PRODUCT_PUBLISHED", "PRODUCT_CREATED", "PRODUCT_UPDATED", "PRODUCT_ACTIVATED",
            "PRODUCT_STATUS_CHANGED");

    private final ObjectMapper objectMapper;
    private final ProductServiceClient productServiceClient;
    private final ProductSearchRepository productSearchRepository;
    private final BrandsRepository brandsRepository;
    private final ManufacturersRepository manufacturersRepository;
    private final CategoryRepository categoryRepository;

    @KafkaListener(topics = "${catalog.search.events.product-topic:product-events}",
            groupId = "${catalog.search.events.group-id:catalog-product-search-projection}",
            concurrency = "${catalog.search.events.concurrency:3}")
    public void onProductEvent(String payload) throws Exception {
        JsonNode event = objectMapper.readTree(payload);
        String eventType = event.path("eventType").asText(event.path("type").asText());
        String normalizedEventType = eventType.replaceAll("([a-z])([A-Z])", "$1_$2")
                .replace('-', '_').toUpperCase(Locale.ROOT);
        String productId = event.path("productId").asText();
        if (productId.isBlank()) {
            throw new IllegalArgumentException("Product event thiếu productId");
        }

        if (DELETE_EVENT_TYPES.contains(normalizedEventType)) {
            productSearchRepository.deleteById(productId);
            return;
        }
        if (!UPSERT_EVENT_TYPES.contains(normalizedEventType)) {
            log.debug("Bỏ qua event không thuộc projection search: {}", eventType);
            return;
        }

        ProductSearchSourceResponse product;
        try {
            product = productServiceClient.getSearchSource(UUID.fromString(productId)).getData();
        } catch (FeignException.NotFound notFound) {
            productSearchRepository.deleteById(productId);
            log.info("Product không còn tồn tại; đã xóa document search, productId={}", productId);
            return;
        }
        if (product == null || product.id() == null) {
            throw new IllegalStateException("Product Service không trả snapshot cho " + productId);
        }
        if (!"ACTIVE".equalsIgnoreCase(product.status())) {
            productSearchRepository.deleteById(productId);
            return;
        }

        ProductSearchSourceResponse.Revision revision = product.currentRevision();
        if (revision == null || revision.name() == null || revision.name().isBlank()) {
            throw new IllegalStateException("Product snapshot chưa có revision có tên: " + productId);
        }
        ProductSearchDocument.ProductSearchDocumentBuilder builder = ProductSearchDocument.builder()
                .productId(product.id().toString())
                .sku(product.productCode().toUpperCase(java.util.Locale.ROOT))
                .name(revision.name())
                .status(product.status())
                .searchText(String.join(" ", safe(product.productCode()), safe(revision.name()),
                        safe(revision.shortDescription()), safe(revision.description())));
        if (product.brandId() != null) {
            brandsRepository.findById(product.brandId().toString()).ifPresent(brand ->
                    builder.brand(BrandSearchDocument.builder().id(brand.getId()).name(brand.getName()).build()));
        }
        if (product.manufacturerId() != null) {
            manufacturersRepository.findById(product.manufacturerId().toString()).ifPresent(manufacturer ->
                    builder.manufacturer(ManufacturerSearchDocument.builder()
                            .id(manufacturer.getId()).name(manufacturer.getName()).build()));
        }
        if (product.categoryId() != null) {
            categoryRepository.findById(product.categoryId().toString()).ifPresent(category ->
                    builder.categories(List.of(CategorySearchDocument.builder().id(category.getId())
                            .name(category.getName()).path(categoryPath(category.getId())).build())));
        }
        productSearchRepository.save(builder.build());
    }

    private String safe(String value) { return value == null ? "" : value; }

    private String categoryPath(String categoryId) {
        List<String> path = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        String currentId = categoryId;
        while (currentId != null && visited.add(currentId) && path.size() < 32) {
            var current = categoryRepository.findById(currentId).orElse(null);
            if (current == null) break;
            path.add(current.getName());
            currentId = current.getParentId();
        }
        Collections.reverse(path);
        return String.join(" ", path);
    }
}
