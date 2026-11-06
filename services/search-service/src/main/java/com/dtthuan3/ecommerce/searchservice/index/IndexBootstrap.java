package com.dtthuan3.ecommerce.searchservice.index;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;

/**
 * Tao index products_v2 voi analyzer tieng Viet CocCoc khi search-service
 * start (idempotent: chi tao khi chua ton tai — PUT index da ton tai se 400).
 *
 * Analyzer chain (docs/vi-search.md §2):
 * 1. vi_analyzer (plugin CocCoc): tach tu tieng Viet dung hanh vi
 *    ("điện thoại" → 1 token "điện thoại", khong phai "điện" + "thoại").
 * 2. asciifolding: token co dau → khong dau ("điện thoại" → "dien thoai")
 *    de khach go khong dau van match. Index va query dung CHUNG analyzer
 *    nen ca 2 phia deu fold — khong can index 2 ban.
 * 3. synonyms: "dtdd" ≈ "điện thoại di động", "iphone" ≈ "apple"... —
 *    ap dung luc index (index_time) nen them synonym phai reindex.
 */
@Configuration
public class IndexBootstrap {

    private static final Logger log = LoggerFactory.getLogger(IndexBootstrap.class);

    @Value("${search.synonyms:}")
    private List<String> synonyms;

    @Bean
    ApplicationRunner indexCreator(ElasticsearchOperations esOps, ObjectMapper objectMapper) {
        return args -> {
            IndexOperations ops = esOps.indexOps(IndexCoordinates.of(ProductDocument.INDEX));
            if (ops.exists()) {
                log.info("Index {} da ton tai — bo qua tao", ProductDocument.INDEX);
                return;
            }

            String settings = objectMapper.writeValueAsString(Map.of(
                    "settings", Map.of(
                            "index", Map.of(
                                    "number_of_shards", 1,
                                    "number_of_replicas", 0
                            ),
                            "analysis", Map.of(
                                    "analyzer", Map.of(
                                            // Analyzer chinh: CocCoc tokenizer + lowercase
                                            // + asciifolding + synonyms + stop VN.
                                            "vi_search", Map.of(
                                                    "type", "custom",
                                                    "tokenizer", "vi_tokenizer",
                                                    "filter", List.of("lowercase",
                                                            "vi_stop", "asciifolding", "vi_synonym")
                                            ),
                                            // Analyzer nhap: giong vi_search nhung
                                            // KHONG synonym o query time — synonym da
                                            // expand o index time, query chi can match.
                                            "vi_query", Map.of(
                                                    "type", "custom",
                                                    "tokenizer", "vi_tokenizer",
                                                    "filter", List.of("lowercase",
                                                            "vi_stop", "asciifolding")
                                            )
                                    ),
                                    "filter", Map.of(
                                            "vi_synonym", Map.of(
                                                    "type", "synonym",
                                                    "synonyms", synonyms
                                            )
                                    )
                            )
                    ),
                    "mappings", Map.of(
                            "properties", Map.ofEntries(
                                    Map.entry("productId", Map.of("type", "keyword")),
                                    Map.entry("productCode", Map.of("type", "keyword")),
                                    Map.entry("name", Map.of(
                                            "type", "text",
                                            "analyzer", "vi_search",
                                            "fields", Map.of("raw", Map.of("type", "keyword"))
                                    )),
                                    Map.entry("nameNoaccent", Map.of(
                                            "type", "text",
                                            "analyzer", "vi_query"
                                    )),
                                    Map.entry("shortDescription", Map.of("type", "text", "analyzer", "vi_search")),
                                    Map.entry("searchText", Map.of("type", "text", "analyzer", "vi_search")),
                                    Map.entry("priceFrom", Map.of("type", "long")),
                                    Map.entry("categoryId", Map.of("type", "keyword")),
                                    Map.entry("brandId", Map.of("type", "keyword")),
                                    Map.entry("primaryImageKey", Map.of("type", "keyword")),
                                    Map.entry("sellableSkuCount", Map.of("type", "integer")),
                                    Map.entry("status", Map.of("type", "keyword")),
                                    Map.entry("updatedAt", Map.of("type", "date"))
                            )
                    )
            ));

            ops.create(objectMapper.readValue(settings, java.util.Map.class));
            log.info("Da tao index {} voi analyzer CocCoc (vi_search) + {} synonyms",
                    ProductDocument.INDEX, synonyms.size());
        };
    }
}
