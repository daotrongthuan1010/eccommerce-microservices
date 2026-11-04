package com.dtthuan3.ecommerce.apigateway.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Regression test cho bug: webhook IPN (payment) va webhook nha xe (shipping)
 * KHONG co JWT (ben ngoai goi vao) nhung bi gateway chan 401 vi rule
 * "/api/** authenticated" khong co exception cho webhook. Service layer
 * tu bao ve bang HMAC + timestamp + dedup.
 *
 * Test chi kich hoat SecurityWebFilterChain (WebFluxTest + Import config),
 * khong can route that: ky vong 401 tu security truoc khi routing.
 */
@WebFluxTest
@Import(SecurityConfig.class)
class GatewayWebhookSecurityTest {

    @Autowired
    private WebTestClient client;

    @Test
    @DisplayName("Webhook payment IPN khong JWT duoc qua gateway (service verify HMAC)")
    void paymentWebhook_withoutJwt_notBlockedByGateway() {
        Integer status = client.post()
                .uri("/api/payment/payments/webhooks/vnpay")
                .header("Content-Type", "application/json")
                .bodyValue("{}")
                .exchange()
                .returnResult(Object.class)
                .getStatus().value();
        // 401 = bug quay lai (gateway chan webhook); 404/503 = qua security, khong route that
        org.assertj.core.api.Assertions.assertThat(status).isNotEqualTo(401);
    }

    @Test
    @DisplayName("Webhook carrier shipping khong JWT duoc qua gateway")
    void shippingWebhook_withoutJwt_notBlockedByGateway() {
        Integer status = client.post()
                .uri("/api/shipping/shipments/webhooks/carrier")
                .header("Content-Type", "application/json")
                .bodyValue("{}")
                .exchange()
                .returnResult(Object.class)
                .getStatus().value();
        org.assertj.core.api.Assertions.assertThat(status).isNotEqualTo(401);
    }

    @Test
    @DisplayName("API thong van (khong phai webhook) khong JWT van bi chan 401")
    void normalApi_withoutJwt_stillBlocked() {
        client.get()
                .uri("/api/payment/payments/some-id")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("Internal API khong bao gio duoc expose public (denyAll)")
    void internalApi_alwaysDenied() {
        Integer status = client.get()
                .uri("/api/auth/internal/something")
                .exchange()
                .returnResult(Object.class)
                .getStatus().value();
        // denyAll + khong JWT: Spring tra 401 (authenticate truoc khi danh gia deny);
        // diem quan trong: KHONG PHAI 200 — khong ai doc duoc internal API
        org.assertj.core.api.Assertions.assertThat(status).isEqualTo(401);
    }

    // ---- Marketplace guest browse (2026-11-05 fe-marketplace D3) ----

    @Test
    @DisplayName("GET catalog/product/review khong JWT duoc qua gateway (guest browse)")
    void guestGet_browsePaths_notBlocked() {
        for (String uri : new String[] {
                "/api/catalog/categories",
                "/api/product/products/browse",
                "/api/review/api/products/some-id/reviews"
        }) {
            Integer status = client.get()
                    .uri(uri)
                    .exchange()
                    .returnResult(Object.class)
                    .getStatus().value();
            // 401 = guest bi chan (bug); 404/503 = qua security, khong route that
            org.assertj.core.api.Assertions.assertThat(status).isNotEqualTo(401);
        }
    }

    @Test
    @DisplayName("Write len catalog/product/review khong JWT van bi chan 401")
    void guestWrite_browsePaths_stillBlocked() {
        client.post()
                .uri("/api/catalog/categories")
                .header("Content-Type", "application/json")
                .bodyValue("{}")
                .exchange()
                .expectStatus().isUnauthorized();
        client.post()
                .uri("/api/product/products")
                .header("Content-Type", "application/json")
                .bodyValue("{}")
                .exchange()
                .expectStatus().isUnauthorized();
        client.post()
                .uri("/api/review/api/reviews")
                .header("Content-Type", "application/json")
                .bodyValue("{}")
                .exchange()
                .expectStatus().isUnauthorized();
    }
}
