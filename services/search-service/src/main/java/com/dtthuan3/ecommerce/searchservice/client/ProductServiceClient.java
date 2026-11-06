package com.dtthuan3.ecommerce.searchservice.client;

import com.dtthuan3.ecommerce.common.response.ApiResponse;
import com.dtthuan3.ecommerce.searchservice.dto.ProductDetailDto;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Goi product-service de enrich event Kafka (chi mang ID) thanh document
 * day du truoc khi index vao ES. RestClient (sync) vi consumer Kafka da
 * chay trong worker rieng. Dia chi qua Eureka: http://product-service.
 * GET /products/** la endpoint public (permitAll) nen khong can token.
 */
@Component
public class ProductServiceClient {

    private static final Logger log = LoggerFactory.getLogger(ProductServiceClient.class);

    private final RestClient restClient;

    public ProductServiceClient(@Qualifier("productServiceRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    /** Lay 1 san pham; null neu 404/loi (consumer quyet dinh bo qua hay retry). */
    public ProductDetailDto getProduct(UUID productId) {
        try {
            ApiResponse<ProductDetailDto> res = restClient.get()
                    .uri("/products/{id}", productId)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });
            return res != null ? res.getData() : null;
        } catch (Exception e) {
            // §4: khong nuot that bai trong lang — log de thay, tra null de
            // caller xu ly (consumer ack bo qua; reindex admin se vay lai).
            log.warn("Enrich product {} that bai: {}", productId, e.getMessage());
            return null;
        }
    }

    /** Lay danh sach san pham PUBLISHED cho reindex toan bo (bootstrap). */
    public List<ProductDetailDto> listPublished(int page, int size) {
        try {
            ApiResponse<PageDto> res = restClient.get()
                    .uri("/products/browse?page={page}&size={size}&sort=newest", page, size)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });
            return res != null && res.getData() != null ? res.getData().content() : List.of();
        } catch (Exception e) {
            log.warn("listPublished page {} that bai: {}", page, e.getMessage());
            return List.of();
        }
    }

    /** PageResponse { content: [...] } theo common PageResponse. */
    public record PageDto(List<ProductDetailDto> content, int page, int size,
                          long totalElements, int totalPages) {
    }
}
