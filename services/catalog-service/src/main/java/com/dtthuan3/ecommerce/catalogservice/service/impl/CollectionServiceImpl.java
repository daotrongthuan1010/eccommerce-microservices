package com.dtthuan3.ecommerce.catalogservice.service.impl;


import com.dtthuan3.ecommerce.catalogservice.contstant.CacheTtl;
import com.dtthuan3.ecommerce.catalogservice.contstant.CollectionStatus;
import com.dtthuan3.ecommerce.catalogservice.dto.request.CollectionCreateRequest;
import com.dtthuan3.ecommerce.catalogservice.dto.request.CollectionReorderRequest;
import com.dtthuan3.ecommerce.catalogservice.dto.request.CollectionUpdateRequest;
import com.dtthuan3.ecommerce.catalogservice.dto.response.CollectionResponse;
import com.dtthuan3.ecommerce.catalogservice.entity.Collection;
import com.dtthuan3.ecommerce.catalogservice.mapper.CollectionMapper;
import com.dtthuan3.ecommerce.catalogservice.repository.CollectionRepository;
import com.dtthuan3.ecommerce.catalogservice.service.CacheKeys;
import com.dtthuan3.ecommerce.catalogservice.service.CollectionService;
import com.dtthuan3.ecommerce.catalogservice.service.RedisCacheService;
import com.dtthuan3.ecommerce.common.exception.BusinessException;
import com.dtthuan3.ecommerce.common.exception.ErrorCode;
import com.dtthuan3.ecommerce.common.exception.ResourceNotFoundException;
import com.dtthuan3.ecommerce.common.response.PageResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CollectionServiceImpl
        implements CollectionService {

    private final CollectionRepository collectionRepository;

    private final CollectionMapper collectionMapper;

    private final RedisCacheService redisCacheService;


    // =========================================================
    // CREATE
    // =========================================================

    @Override
    @Transactional
    public CollectionResponse create(
            CollectionCreateRequest request
    ) {

        String slug = normalizeSlug(
                request.getSlug()
        );

        if (collectionRepository.existsBySlug(slug)) {

            throw new BusinessException(
                    ErrorCode.CONFLICT,
                    "Slug collection đã tồn tại"
            );
        }

        validateTimeRange(
                request.getStartAt(),
                request.getEndAt()
        );

        Collection collection =
                collectionMapper.toEntity(request);

        collection.setSlug(slug);

        if (collection.getDisplayOrder() == null) {
            collection.setDisplayOrder(0);
        }

        if (collection.getStatus() == null) {
            collection.setStatus(
                    CollectionStatus.DRAFT
            );
        }

        if (collection.getRevision() == null) {
            collection.setRevision(0L);
        }

        Collection saved =
                collectionRepository.save(collection);

        evictCollectionListCaches();

        return collectionMapper.toResponse(saved);
    }


    // =========================================================
    // GET BY ID
    // =========================================================

    @Override
    public CollectionResponse getById(
            String id
    ) {

        String cacheKey =
                CacheKeys.COLLECTION + id;

        Optional<CollectionResponse> cached =
                redisCacheService.get(
                        cacheKey,
                        CollectionResponse.class
                );

        if (cached.isPresent()) {
            return cached.get();
        }

        Collection collection =
                findCollection(id);

        CollectionResponse response =
                collectionMapper.toResponse(
                        collection
                );

        redisCacheService.set(
                cacheKey,
                response,
                CacheTtl.MEDIUM
        );

        return response;
    }


    @Override
    public PageResponse<CollectionResponse> getCollections(
            CollectionStatus status,
            String keyword,
            int page,
            int size
    ) {

        validatePage(page, size);

        String normalizedKeyword =
                normalizeKeyword(keyword);

        String cacheKey =
                buildListCacheKey(
                        status,
                        normalizedKeyword,
                        page,
                        size
                );

        Optional<PageResponse<CollectionResponse>> cached =
                redisCacheService.get(
                        cacheKey,
                        new TypeReference<PageResponse<CollectionResponse>>() {
                        }
                );

        if (cached.isPresent()) {
            return cached.get();
        }

        Pageable pageable =
                PageRequest.of(
                        page,
                        size,
                        Sort.by(
                                Sort.Order.asc("displayOrder"),
                                Sort.Order.asc("name"),
                                Sort.Order.asc("id")
                        )
                );

        Page<Collection> result;

        if (status != null) {

            result =
                    collectionRepository.findByStatus(
                            status,
                            pageable
                    );

        } else if (normalizedKeyword != null) {

            result =
                    collectionRepository
                            .findByNameContainingIgnoreCase(
                                    normalizedKeyword,
                                    pageable
                            );

        } else {

            result =
                    collectionRepository.findAll(
                            pageable
                    );
        }

        List<CollectionResponse> content =
                result.getContent()
                        .stream()
                        .map(collectionMapper::toResponse)
                        .toList();

        PageResponse<CollectionResponse> response =
                PageResponse.of(
                        content,
                        result.getNumber(),
                        result.getSize(),
                        result.getTotalElements(),
                        result.getTotalPages()
                );

        redisCacheService.set(
                cacheKey,
                response,
                CacheTtl.MEDIUM
        );

        return response;
    }



    @Override
    @Transactional
    public CollectionResponse update(
            String id,
            CollectionUpdateRequest request
    ) {

        Collection collection =
                findCollection(id);

        String slug =
                normalizeSlug(request.getSlug());

        if (collectionRepository
                .existsBySlugAndIdNot(
                        slug,
                        id
                )) {

            throw new BusinessException(
                    ErrorCode.CONFLICT,
                    "Slug collection đã tồn tại"
            );
        }

        validateTimeRange(
                request.getStartAt(),
                request.getEndAt()
        );

        collectionMapper.updateEntity(
                request,
                collection
        );

        collection.setSlug(slug);

        incrementRevision(collection);

        Collection saved =
                collectionRepository.save(
                        collection
                );

        evictCollectionCaches(id);

        return collectionMapper.toResponse(
                saved
        );
    }


    @Override
    @Transactional
    public CollectionResponse updateStatus(
            String id,
            CollectionStatus status
    ) {

        if (status == null) {

            throw new BusinessException(
                    ErrorCode.BAD_REQUEST,
                    "Trạng thái không được để trống"
            );
        }

        Collection collection =
                findCollection(id);

        collection.setStatus(status);

        incrementRevision(collection);

        Collection saved =
                collectionRepository.save(collection);

        evictCollectionCaches(id);

        return collectionMapper.toResponse(saved);
    }


    @Override
    @Transactional
    public void delete(
            String id
    ) {

        Collection collection =
                findCollection(id);

        if (collection.getStatus()
                != CollectionStatus.ARCHIVED) {

            throw new BusinessException(
                    ErrorCode.BAD_REQUEST,
                    "Chỉ được xóa collection ở trạng thái ARCHIVED"
            );
        }

        collectionRepository.delete(collection);

        evictCollectionCaches(id);
    }

    @Override
    public List<CollectionResponse>
    getActiveCollections() {

        String cacheKey =
                CacheKeys.COLLECTION_ACTIVE;

        List<CollectionResponse> cached =
                redisCacheService.getList(
                        cacheKey,
                        CollectionResponse.class
                );

        if (!cached.isEmpty()
                || redisCacheService.hasKey(cacheKey)) {

            return cached;
        }

        List<CollectionResponse> response =
                collectionRepository
                        .findByStatusOrderByDisplayOrderAscIdAsc(
                                CollectionStatus.ACTIVE
                        )
                        .stream()
                        .map(collectionMapper::toResponse)
                        .toList();

        redisCacheService.set(
                cacheKey,
                response,
                CacheTtl.MEDIUM
        );

        return response;
    }


    @Override
    public List<CollectionResponse>
    getHomepageCollections() {

        String cacheKey =
                CacheKeys.COLLECTION_HOMEPAGE;

        List<CollectionResponse> cached =
                redisCacheService.getList(
                        cacheKey,
                        CollectionResponse.class
                );

        if (!cached.isEmpty()
                || redisCacheService.hasKey(cacheKey)) {

            return cached;
        }

        LocalDateTime now =
                LocalDateTime.now();

        List<CollectionResponse> response =
                collectionRepository
                        .findHomepageCollections(
                                CollectionStatus.ACTIVE,
                                now
                        )
                        .stream()
                        .map(collectionMapper::toResponse)
                        .toList();

        redisCacheService.set(
                cacheKey,
                response,
                CacheTtl.MEDIUM
        );

        return response;
    }


    @Override
    @Transactional
    public CollectionResponse publish(
            String id
    ) {

        Collection collection =
                findCollection(id);

        validatePublish(collection);

        collection.setStatus(
                CollectionStatus.ACTIVE
        );

        incrementRevision(collection);

        Collection saved =
                collectionRepository.save(collection);

        evictCollectionCaches(id);

        /*
         * Sau này:
         *
         * Kafka:
         * CollectionPublishedEvent
         *
         * revision được gửi kèm event.
         */

        return collectionMapper.toResponse(saved);
    }


    @Override
    @Transactional
    public CollectionResponse unpublish(
            String id
    ) {

        Collection collection =
                findCollection(id);

        collection.setStatus(
                CollectionStatus.HIDDEN
        );

        incrementRevision(collection);

        Collection saved =
                collectionRepository.save(collection);

        evictCollectionCaches(id);

        return collectionMapper.toResponse(saved);
    }


    @Override
    @Transactional
    public void reorder(
            CollectionReorderRequest request
    ) {

        List<String> ids =
                request.getCollectionIds();

        if (ids == null || ids.isEmpty()) {

            throw new BusinessException(
                    ErrorCode.BAD_REQUEST,
                    "Danh sách collection không được để trống"
            );
        }

        /*
         * SET dùng để phát hiện ID trùng.
         *
         * List vẫn giữ thứ tự.
         */
        Set<String> uniqueIds =
                new HashSet<>(ids);

        if (uniqueIds.size() != ids.size()) {

            throw new BusinessException(
                    ErrorCode.BAD_REQUEST,
                    "Danh sách collection chứa ID trùng"
            );
        }

        /*
         * Một query DB duy nhất.
         *
         * Không find từng ID.
         */
        List<Collection> collections =
                collectionRepository.findAllById(ids);

        if (collections.size() != ids.size()) {

            throw new ResourceNotFoundException(
                    "Một hoặc nhiều collection không tồn tại"
            );
        }

        /*
         * MAP:
         *
         * collectionId -> Collection
         *
         * Lookup O(1)
         */
        Map<String, Collection> collectionMap =
                new HashMap<>(collections.size());

        for (Collection collection : collections) {

            collectionMap.put(
                    collection.getId(),
                    collection
            );
        }

        /*
         * Gán lại displayOrder
         */
        for (int index = 0;
             index < ids.size();
             index++) {

            Collection collection =
                    collectionMap.get(
                            ids.get(index)
                    );

            collection.setDisplayOrder(
                    index + 1
            );

            incrementRevision(collection);
        }

        /*
         * Một lần saveAll.
         */
        collectionRepository.saveAll(
                collections
        );

        evictCollectionListCaches();
    }


    private Collection findCollection(
            String id
    ) {

        return collectionRepository
                .findById(id)
                .orElseThrow(
                        () -> new ResourceNotFoundException(
                                "Không tìm thấy collection: " + id
                        )
                );
    }


    private void validatePage(
            int page,
            int size
    ) {

        if (page < 0) {

            throw new BusinessException(
                    ErrorCode.BAD_REQUEST,
                    "Page không được nhỏ hơn 0"
            );
        }

        if (size <= 0 || size > 100) {

            throw new BusinessException(
                    ErrorCode.BAD_REQUEST,
                    "Size phải nằm trong khoảng từ 1 đến 100"
            );
        }
    }


    private void validateTimeRange(
            LocalDateTime startAt,
            LocalDateTime endAt
    ) {

        if (startAt != null
                && endAt != null
                && !endAt.isAfter(startAt)) {

            throw new BusinessException(
                    ErrorCode.BAD_REQUEST,
                    "endAt phải sau startAt"
            );
        }
    }

    private void validatePublish(
            Collection collection
    ) {

        if (collection.getStartAt() == null) {

            throw new BusinessException(
                    ErrorCode.BAD_REQUEST,
                    "Collection phải có startAt trước khi publish"
            );
        }

        if (collection.getEndAt() != null
                && !collection.getEndAt()
                .isAfter(collection.getStartAt())) {

            throw new BusinessException(
                    ErrorCode.BAD_REQUEST,
                    "endAt phải sau startAt"
            );
        }
    }


    private void incrementRevision(
            Collection collection
    ) {

        Long revision =
                collection.getRevision();

        if (revision == null) {
            revision = 0L;
        }

        collection.setRevision(
                revision + 1
        );
    }


    private String normalizeSlug(
            String slug
    ) {

        if (slug == null) {
            return null;
        }

        return slug
                .trim()
                .toLowerCase();
    }

    private String normalizeKeyword(
            String keyword
    ) {

        if (keyword == null
                || keyword.isBlank()) {

            return null;
        }

        return keyword.trim();
    }


    private String buildListCacheKey(
            CollectionStatus status,
            String keyword,
            int page,
            int size
    ) {

        String statusValue =
                status == null
                        ? "ALL"
                        : status.name();

        String keywordValue =
                keyword == null
                        ? ""
                        : keyword.toLowerCase();

        return CacheKeys.COLLECTION_LIST
                + "status=" + statusValue
                + ":keyword=" + keywordValue
                + ":page=" + page
                + ":size=" + size;
    }


    private void evictCollectionCaches(
            String id
    ) {

        redisCacheService.delete(
                CacheKeys.COLLECTION + id
        );

        evictCollectionListCaches();
    }

    private void evictCollectionListCaches() {

        redisCacheService.delete(
                CacheKeys.COLLECTION_ACTIVE
        );

        redisCacheService.delete(
                CacheKeys.COLLECTION_HOMEPAGE
        );

        redisCacheService.deleteByPattern(
                CacheKeys.COLLECTION_LIST + "*"
        );
    }
}