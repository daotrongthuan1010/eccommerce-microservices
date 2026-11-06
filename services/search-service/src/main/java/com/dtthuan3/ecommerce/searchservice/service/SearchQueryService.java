package com.dtthuan3.ecommerce.searchservice.service;

import com.dtthuan3.ecommerce.searchservice.index.ProductDocument;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.stereotype.Service;

/**
 * Query ES cho tim kiem tieng Viet (docs/vi-search.md §4):
 * - multi_match tren name (vi_search) + searchText + nameNoaccent
 *   → khach go co dau, khong dau deu match (asciifolding 2 phia).
 * - fuzzy AUTO tren truong name: go sai 1-2 ky tu van ra.
 * - synonym da expand o index time nen query khong can lam gi them.
 */
@Service
public class SearchQueryService {

    private final ElasticsearchTemplate esTemplate;

    public SearchQueryService(ElasticsearchTemplate esTemplate) {
        this.esTemplate = esTemplate;
    }

    public record SearchHitDto(
            String productId, String productCode, String name, String shortDescription,
            long priceFrom, String categoryId, String brandId, String primaryImageKey,
            int sellableSkuCount, double score) {
    }

    public record SearchResult(List<SearchHitDto> hits, long total) {
    }

    public SearchResult search(String keyword, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);

        // Query native JSON de dung multi_match + fuzzy (Criteria API khong
        // ho tro fuzzy) — van qua ElasticsearchTemplate de giu type safety.
        Map<String, Object> multiMatch = Map.of(
                "query", keyword,
                "fields", List.of("name^3", "searchText", "nameNoaccent^2"),
                "type", "best_fields",
                "fuzziness", "AUTO",
                "operator", "and"
        );

        Map<String, Object> queryBody = Map.of(
                "bool", Map.of(
                        "must", List.of(Map.of("multi_match", multiMatch)),
                        "filter", List.of(Map.of("term", Map.of("status", "ACTIVE")))
                )
        );

        Query query = new org.springframework.data.elasticsearch.core.query.StringQuery(
                toJson(queryBody))
                .setPageable(PageRequest.of(safePage, safeSize))
                .addSort(Sort.by(Sort.Direction.DESC, "_score"));

        SearchHits<java.util.Map> hits = esTemplate.search(
                query, java.util.Map.class,
                org.springframework.data.elasticsearch.core.mapping.IndexCoordinates
                        .of(ProductDocument.INDEX));

        List<SearchHitDto> dtos = hits.getSearchHits().stream()
                .map(ProductEventConsumerHelper::toDto)
                .toList();
        return new SearchResult(dtos, hits.getTotalHits());
    }

    private static String toJson(Map<String, Object> body) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(body);
        } catch (Exception e) {
            throw new IllegalStateException("Khong serialize duoc query ES", e);
        }
    }

    /** Helper map hit source → DTO (tach de test duoc). */
    static final class ProductEventConsumerHelper {
        private ProductEventConsumerHelper() {
        }

        static SearchHitDto toDto(SearchHit<java.util.Map> hit) {
            java.util.Map<String, Object> src = hit.getContent();
            return new SearchHitDto(
                    str(src.get("productId")),
                    str(src.get("productCode")),
                    str(src.get("name")),
                    str(src.get("shortDescription")),
                    src.get("priceFrom") instanceof Number n ? n.longValue() : 0L,
                    str(src.get("categoryId")),
                    str(src.get("brandId")),
                    str(src.get("primaryImageKey")),
                    src.get("sellableSkuCount") instanceof Number n ? n.intValue() : 0,
                    hit.getScore());
        }

        private static String str(Object o) {
            return o == null ? null : String.valueOf(o);
        }
    }
}
