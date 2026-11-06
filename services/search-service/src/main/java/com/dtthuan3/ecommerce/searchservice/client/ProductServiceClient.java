package com.dtthuan3.ecommerce.searchservice.client;

import com.dtthuan3.ecommerce.searchservice.dto.ApiEnvelope;
import com.dtthuan3.ecommerce.searchservice.dto.ProductDetailDto;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Goi product-service de enrich event Kafka (chi mang ID) thanh document
 * day du truoc khi index vao ES. Dung RestClient (sync) vi consumer Kafka
 * da chay trong worker rieng — khong can WebClient reactive.
 *
 * Dia chi qua Eureka: http://product-service (service discovery trong cluster).
 * GET /products/{id} la endpoint public (permitAll) nen khong can token.
 */
@Component
public class ProductServiceClient {

    private final RestClient restClient;

    public ProductServiceClient(@Qualifier("productServiceRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    /** Lay 1 san pham; tra null neu 404/loi de consumer bo qua event an toan. */
    public ProductDetailDto getProduct(UUID productId) {
        try {
            ApiEnvelope<ProductDetailDto> res = restClient.get()
                    .uri("/products/{id}", productId)
                    .retrieve()
                    .body(new org.springframework.core.ParameterizedTypeReference<>() {
                    });
            return res != null ? res.data() : null;
        } catch (Exception e) {
            // Enrich fail (product-service down, 404 sau khi xoa...): tra null,
            // caller quyet dinh retry hay bo qua — khong lam crash consumer.
            return null;
        }
    }

    /** Lay danh sach san pham PUBLISHED cho reindex toan bo (bootstrap). */
    public List<ProductDetailDto> listPublished(int page, int size) {
        try {
            ApiEnvelope<PageDto> res = restClient.get()
                    .uri("/products/browse?page={page}&size={size}&sort=newest", page, size)
                    .retrieve()
                    .body(new org.springframework.core.ParameterizedTypeReference<>() {
                    });
            return res != null && res.data() != null ? res.data().content() : List.of();
        } catch (Exception e) {
            return List.of();
        }
    }

    /** PageResponse { content: [...] } theo common PageResponse. */
    public record PageDto(List<ProductDetailDto> content, int page, int size,
                          long totalElements, int totalPages) {
    }
}
