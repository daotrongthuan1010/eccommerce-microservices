package com.dtthuan3.ecommerce.searchservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * View san pham khi search-service goi product-service de enrich event
 * (event Kafka chi mang ID — enrichment pattern, giam payload tren topic).
 * 2 nguon, 2 shape:
 * - GET /products/{id} (detail): ten nam currentRevision.name
 * - GET /products/browse (reindex): ten nam name (top-level, da resolve
 *   published revision) — khong co currentRevision → phai co ca 2 field.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ProductDetailDto(
        String id,
        String productCode,
        String categoryId,
        String brandId,
        String status,
        long priceFrom,
        String name,
        String shortDescription,
        Revision currentRevision) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Revision(String name, String shortDescription) {
    }

    /** Ten hien thi uu tien revision (detail), roi top-level name (browse). */
    public String displayName() {
        return currentRevision != null && currentRevision.name() != null
                ? currentRevision.name()
                : (name != null ? name : productCode);
    }

    public String displayShortDescription() {
        if (currentRevision != null && currentRevision.shortDescription() != null) {
            return currentRevision.shortDescription();
        }
        return shortDescription;
    }
}
