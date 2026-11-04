package com.dtthuan3.ecommerce.reviewservice.client;

import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Optional;


@Component
public class OrderLineClient {

    public record OrderLineInfo(String lineId, String orderId, String userId, String productId, String skuId,
                                String status, String deliveredAt) {
    }

    /** Bật khi order-service có GET /internal/order-lines/{lineId}. */
    private static final boolean ORDER_FALLBACK_ENABLED = false;

    private final RestClient rest;

    public OrderLineClient(RestClient.Builder lbBuilder) {
        this.rest = lbBuilder.clone().baseUrl("http://order-service").build();
    }

    public boolean enabled() {
        return ORDER_FALLBACK_ENABLED;
    }

    /** Rỗng nếu tắt, không tìm thấy hoặc Order-service lỗi (khi đó người dùng được báo "đang đồng bộ"). */
    public Optional<OrderLineInfo> find(String lineId) {
        if (!enabled()) {
            return Optional.empty();
        }
        try {
            return Optional.ofNullable(rest.get()
                    .uri("/internal/order-lines/{lineId}", lineId)
                    .headers(this::bearer)
                    .retrieve()
                    .body(OrderLineInfo.class));
        } catch (RestClientException e) {
            return Optional.empty();
        }
    }

    private void bearer(HttpHeaders headers) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwt) {
            headers.setBearerAuth(jwt.getToken().getTokenValue());
        }
    }
}
