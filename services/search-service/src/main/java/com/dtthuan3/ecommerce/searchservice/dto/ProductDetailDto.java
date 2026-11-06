package com.dtthuan3.ecommerce.searchservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * View san pham khi search-service goi lai product-service de enrich event
 * (event Kafka chi mang ID — enrichment pattern, giam payload tren topic).
 * Fields theo ProductResponse cua product-service.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ProductDetailDto(
        String id,
        String productCode,
        String categoryId,
        String brandId,
        String status,
        long priceFrom,
        Revision currentRevision) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Revision(String name, String shortDescription) {
    }
}
