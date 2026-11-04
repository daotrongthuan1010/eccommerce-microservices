package com.dtthuan3.ecommerce.shippingservice.dto.response;

import com.dtthuan3.ecommerce.shippingservice.entity.ShippingQuote;
import java.time.Instant;

public record QuoteResponse(
        String quoteId, String zone, int billableWeightGrams, long fee, String currency, Instant expiresAt) {

    public static QuoteResponse from(ShippingQuote q) {
        return new QuoteResponse(q.getId(), q.getZone(), q.getBillableWeightGrams(), q.getFee(), q.getCurrency(), q.getExpiresAt());
    }
}
