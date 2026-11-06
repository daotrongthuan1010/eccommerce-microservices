package com.dtthuan3.ecommerce.searchservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Envelope ApiResponse chuan cua common module (FE_API_CONTRACT §0.3) —
 * search-service chi parse data, khong ke thu common de giu dependency nho.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ApiEnvelope<T>(boolean success, String code, String message, T data) {
}
