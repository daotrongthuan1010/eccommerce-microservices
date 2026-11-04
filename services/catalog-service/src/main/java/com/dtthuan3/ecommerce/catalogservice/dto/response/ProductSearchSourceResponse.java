package com.dtthuan3.ecommerce.catalogservice.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ProductSearchSourceResponse(
        UUID id,
        String productCode,
        UUID categoryId,
        UUID brandId,
        UUID manufacturerId,
        String status,
        Revision currentRevision
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Revision(String name, String shortDescription, String description) { }
}
