package com.dtthuan3.ecommerce.catalogservice.service.impl;

import com.dtthuan3.ecommerce.catalogservice.contstant.BrandsStatus;
import com.dtthuan3.ecommerce.catalogservice.contstant.CacheTtl;
import com.dtthuan3.ecommerce.catalogservice.dto.request.BrandsCreateRequest;
import com.dtthuan3.ecommerce.catalogservice.dto.request.BrandsUpdateRequest;
import com.dtthuan3.ecommerce.catalogservice.dto.response.BrandsResponse;
import com.dtthuan3.ecommerce.catalogservice.entity.Brands;
import com.dtthuan3.ecommerce.catalogservice.mapper.BrandsMapper;
import com.dtthuan3.ecommerce.catalogservice.repository.BrandsRepository;
import com.dtthuan3.ecommerce.catalogservice.service.BrandsService;
import com.dtthuan3.ecommerce.catalogservice.service.CacheKeys;
import com.dtthuan3.ecommerce.catalogservice.service.RedisCacheService;
import com.dtthuan3.ecommerce.common.exception.BusinessException;
import com.dtthuan3.ecommerce.common.exception.ErrorCode;
import com.dtthuan3.ecommerce.common.exception.ResourceNotFoundException;
import com.dtthuan3.ecommerce.common.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BrandsServiceImpl implements BrandsService {

    private final BrandsRepository brandsRepository;
    private final BrandsMapper brandsMapper;
    private final RedisCacheService redisCacheService;


    @Override
    @Transactional
    public BrandsResponse create(BrandsCreateRequest request) {

        String slug = normalizeSlug(request.getSlug());

        if (brandsRepository.existsBySlug(slug)) {
            throw new BusinessException(
                    ErrorCode.CONFLICT,
                    "Slug thương hiệu đã tồn tại: " + slug
            );
        }

        Brands brands = brandsMapper.toEntity(request);

        brands.setSlug(slug);

        if (brands.getRevision() == null) {
            brands.setRevision(0L);
        }

        Brands saved = brandsRepository.save(brands);

        // CREATE làm thay đổi các danh sách cache
        evictBrandsListCaches();

        return brandsMapper.toResponse(saved);
    }


    @Override
    public BrandsResponse getById(String id) {

        String cacheKey = CacheKeys.BRANDS + id;

        Optional<BrandsResponse> cached =
                redisCacheService.get(
                        cacheKey,
                        BrandsResponse.class
                );

        if (cached.isPresent()) {
            return cached.get();
        }

        Brands brands = findBrands(id);

        BrandsResponse response =
                brandsMapper.toResponse(brands);

        redisCacheService.set(
                cacheKey,
                response,
                CacheTtl.MEDIUM
        );

        return response;
    }


    @Override
    public PageResponse<BrandsResponse> getBrands(
            BrandsStatus status,
            String name,
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

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Order.asc("name"),
                        Sort.Order.asc("id")
                )
        );

        Page<Brands> result;

        if (status != null) {

            result = brandsRepository.findByStatus(
                    status,
                    pageable
            );

        } else if (name != null && !name.isBlank()) {

            result = brandsRepository.findByNameContainingIgnoreCase(
                    name.trim(),
                    pageable
            );

        } else {

            result = brandsRepository.findAll(pageable);
        }

        List<BrandsResponse> content =
                result.getContent()
                        .stream()
                        .map(brandsMapper::toResponse)
                        .toList();

        return PageResponse.of(
                content,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }


    @Override
    @Transactional
    public BrandsResponse update(
            String id,
            BrandsUpdateRequest request
    ) {

        Brands brands = findBrands(id);

        String slug = normalizeSlug(request.getSlug());

        if (brandsRepository.existsBySlugAndIdNot(slug, id)) {
            throw new BusinessException(
                    ErrorCode.CONFLICT,
                    "Slug thương hiệu đã tồn tại: " + slug
            );
        }

        brands.setName(request.getName());
        brands.setSlug(slug);
        brands.setDescription(request.getDescription());
        brands.setLogoObjectKey(request.getLogoObjectKey());
        brands.setWebsite(request.getWebsite());

        if (request.getStatus() != null) {
            brands.setStatus(request.getStatus());
        }

        Long currentRevision = brands.getRevision();

        if (currentRevision == null) {
            currentRevision = 0L;
        }

        brands.setRevision(currentRevision + 1);

        Brands updated = brandsRepository.save(brands);

        // Xóa cache object
        redisCacheService.delete(
                CacheKeys.BRANDS + id
        );

        // Xóa cache các danh sách
        evictBrandsListCaches();

        return brandsMapper.toResponse(updated);
    }


    @Override
    @Transactional
    public void delete(String id) {

        Brands brands = findBrands(id);

        if (brands.getStatus() != BrandsStatus.ARCHIVED) {
            throw new BusinessException(
                    ErrorCode.BAD_REQUEST,
                    "Chỉ được xóa thương hiệu ở trạng thái ARCHIVED"
            );
        }

        brandsRepository.delete(brands);

        // Xóa cache object
        redisCacheService.delete(
                CacheKeys.BRANDS + id
        );

        // Xóa cache list
        evictBrandsListCaches();
    }


    @Override
    @Transactional
    public BrandsResponse updateStatus(
            String id,
            BrandsStatus status
    ) {

        if (status == null) {
            throw new BusinessException(
                    ErrorCode.BAD_REQUEST,
                    "Trạng thái không được để trống"
            );
        }

        Brands brands = findBrands(id);

        brands.setStatus(status);

        Long currentRevision = brands.getRevision();

        if (currentRevision == null) {
            currentRevision = 0L;
        }

        brands.setRevision(currentRevision + 1);

        Brands updated = brandsRepository.save(brands);

        // Status thay đổi → object cache + list cache đều phải xóa
        redisCacheService.delete(
                CacheKeys.BRANDS + id
        );

        evictBrandsListCaches();

        return brandsMapper.toResponse(updated);
    }



    @Override
    public List<BrandsResponse> getActiveBrands() {

        String cacheKey =
                CacheKeys.BRANDS_ACTIVE;

        List<BrandsResponse> cached =
                redisCacheService.getList(
                        cacheKey,
                        BrandsResponse.class
                );


        if (!cached.isEmpty()
                || redisCacheService.hasKey(cacheKey)) {

            return cached;
        }

        List<BrandsResponse> result =
                brandsRepository
                        .findByStatusOrderByNameAsc(
                                BrandsStatus.ACTIVE
                        )
                        .stream()
                        .map(brandsMapper::toResponse)
                        .toList();

        redisCacheService.set(
                cacheKey,
                result,
                CacheTtl.MEDIUM
        );

        return result;
    }


    @Override
    public List<BrandsResponse> search(String keyword) {

        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }

        String normalizedKeyword =
                keyword.trim().toLowerCase();

        String cacheKey =
                CacheKeys.BRANDS_SEARCH
                        + normalizedKeyword;

        List<BrandsResponse> cached =
                redisCacheService.getList(
                        cacheKey,
                        BrandsResponse.class
                );

        if (!cached.isEmpty()
                || redisCacheService.hasKey(cacheKey)) {

            return cached;
        }

        List<BrandsResponse> result =
                brandsRepository
                        .findByNameContainingIgnoreCase(
                                keyword.trim(),
                                PageRequest.of(
                                        0,
                                        20,
                                        Sort.by("name").ascending()
                                )
                        )
                        .getContent()
                        .stream()
                        .map(brandsMapper::toResponse)
                        .toList();

        redisCacheService.set(
                cacheKey,
                result,
                CacheTtl.MEDIUM
        );

        return result;
    }


    private Brands findBrands(String id) {

        return brandsRepository
                .findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Brands",
                                id
                        )
                );
    }


    private void evictBrandsListCaches() {

        redisCacheService.delete(
                CacheKeys.BRANDS_LIST
        );

        redisCacheService.delete(
                CacheKeys.BRANDS_ACTIVE
        );

        /*
         * Search cache có nhiều key:
         *
         * catalog:brands:search:apple
         * catalog:brands:search:samsung
         * catalog:brands:search:xiaomi
         *
         * Khi CRUD thì xóa toàn bộ search cache.
         */
        redisCacheService.deleteByPattern(
                CacheKeys.BRANDS_SEARCH + "*"
        );
    }


    private String normalizeSlug(String slug) {

        if (slug == null) {
            return null;
        }

        return slug
                .trim()
                .toLowerCase();
    }
}