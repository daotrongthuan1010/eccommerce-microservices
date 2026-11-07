package com.dtthuan3.ecommerce.reviewservice.service;

import com.dtthuan3.ecommerce.common.exception.BusinessException;
import com.dtthuan3.ecommerce.common.exception.ErrorCode;
import com.dtthuan3.ecommerce.common.exception.ResourceNotFoundException;
import com.dtthuan3.ecommerce.common.response.PageResponse;
import com.dtthuan3.ecommerce.reviewservice.dto.request.BannedWordRequest;
import com.dtthuan3.ecommerce.reviewservice.dto.response.BannedWordResponse;
import com.dtthuan3.ecommerce.reviewservice.entity.BannedWord;
import com.dtthuan3.ecommerce.reviewservice.repository.BannedWordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

/**
 * Quản lý từ cấm + cache in-memory cho ReviewRules.
 * Cache refresh tối đa mỗi 60s (lazy) hoặc ép qua reload().
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BannedWordService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final long CACHE_TTL_MS = 60_000;

    private final BannedWordRepository repository;

    /** Snapshot từ cấm (lowercase); volatile để đọc không cần lock. */
    private volatile List<String> cachedWords = List.of();
    private volatile long loadedAt = 0;

    /** Danh sách từ cấm cho kiểm duyệt; lỗi DB thì dùng cache cũ, không chặn tạo review. */
    public List<String> getWords() {
        if (System.currentTimeMillis() - loadedAt > CACHE_TTL_MS) {
            synchronized (this) {
                if (System.currentTimeMillis() - loadedAt > CACHE_TTL_MS) {
                    reload();
                }
            }
        }
        return cachedWords;
    }

    /** Ép tải lại cache từ DB (sau CRUD hoặc qua endpoint /reload). */
    public synchronized void reload() {
        try {
            cachedWords = repository.findAll().stream()
                    .map(BannedWord::getWord)
                    .filter(w -> w != null && !w.isBlank())
                    .map(w -> w.toLowerCase(Locale.ROOT))
                    .toList();
            loadedAt = System.currentTimeMillis();
        } catch (Exception e) {
            log.warn("Không tải được banned_words, dùng cache cũ ({} từ): {}", cachedWords.size(), e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<BannedWordResponse> search(String search, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Page<BannedWord> result = (search == null || search.isBlank())
                ? repository.findAll(PageRequest.of(safePage, safeSize))
                : repository.findByWordContainingIgnoreCase(search.trim(), PageRequest.of(safePage, safeSize));
        List<BannedWordResponse> content = result.getContent().stream().map(this::toResponse).toList();
        return PageResponse.of(content, safePage, safeSize, result.getTotalElements(), result.getTotalPages());
    }

    @Transactional
    public BannedWordResponse create(String createdBy, BannedWordRequest request) {
        String word = normalize(request.word());
        if (word.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Từ cấm không được để trống");
        }
        if (repository.findByWordIgnoreCase(word).isPresent()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Từ cấm đã tồn tại: " + word);
        }
        BannedWord saved = repository.save(BannedWord.builder()
                .word(word)
                .note(request.note() == null || request.note().isBlank() ? null : request.note().trim())
                .createdBy(createdBy)
                .build());
        reload(); // có từ mới -> kiểm duyệt dùng ngay
        return toResponse(saved);
    }

    @Transactional
    public void delete(Long id) {
        BannedWord existing = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy từ cấm"));
        repository.delete(existing);
        reload();
    }

    /** Chuẩn hóa: trim + lowercase (khớp cách so sánh trong ReviewRules). */
    public static String normalize(String word) {
        return word == null ? "" : word.trim().toLowerCase(Locale.ROOT);
    }

    private BannedWordResponse toResponse(BannedWord e) {
        return new BannedWordResponse(e.getId(), e.getWord(), e.getNote(), e.getCreatedAt());
    }
}
