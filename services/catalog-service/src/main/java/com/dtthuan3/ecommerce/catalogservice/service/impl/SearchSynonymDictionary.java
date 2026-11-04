package com.dtthuan3.ecommerce.catalogservice.service.impl;

import com.dtthuan3.ecommerce.catalogservice.contstant.SynonymSetStatus;
import com.dtthuan3.ecommerce.catalogservice.repository.SynonymRepository;
import com.dtthuan3.ecommerce.catalogservice.repository.SynonymSetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/** In-memory, periodically refreshed dictionary so search requests do not query SQL. */
@Slf4j
@Component
@RequiredArgsConstructor
@EnableScheduling
public class SearchSynonymDictionary {

    private static final int MAX_EXPANSIONS_PER_TERM = 10;
    private final SynonymSetRepository synonymSetRepository;
    private final SynonymRepository synonymRepository;
    private final AtomicReference<Map<String, List<String>>> dictionary = new AtomicReference<>(Map.of());

    @Scheduled(initialDelayString = "${catalog.search.synonyms.initial-delay-ms:1000}",
            fixedDelayString = "${catalog.search.synonyms.refresh-ms:60000}")
    public void refresh() {
        try {
            List<String> activeSetIds = synonymSetRepository.findAllByStatus(SynonymSetStatus.ACTIVE)
                    .stream().map(set -> set.getId()).toList();
            Map<String, List<String>> next = new LinkedHashMap<>();
            if (!activeSetIds.isEmpty()) {
                synonymRepository.findBySynonymSetIdIn(activeSetIds).forEach(rule -> {
                    add(next, rule.getTerm(), rule.getSynonym());
                    add(next, rule.getSynonym(), rule.getTerm());
                });
            }
            Map<String, List<String>> immutable = new LinkedHashMap<>();
            next.forEach((key, values) -> immutable.put(key, List.copyOf(values)));
            dictionary.set(Map.copyOf(immutable));
        } catch (RuntimeException ex) {
            log.warn("Không thể làm mới synonym cho catalog search; tiếp tục dùng bản đang có", ex);
        }
    }

    public List<String> expand(String term) {
        return dictionary.get().getOrDefault(normalize(term), List.of());
    }

    private void add(Map<String, List<String>> target, String source, String candidate) {
        String key = normalize(source);
        String value = normalize(candidate);
        if (key.isBlank() || value.isBlank() || key.equals(value)) return;
        List<String> values = target.computeIfAbsent(key, ignored -> new ArrayList<>());
        if (values.size() < MAX_EXPANSIONS_PER_TERM && !values.contains(value)) values.add(value);
    }

    private String normalize(String value) {
        return value == null ? "" : value.strip().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }
}
