package com.dtthuan3.ecommerce.catalogservice.service.impl;


import com.dtthuan3.ecommerce.catalogservice.contstant.CacheTtl;
import com.dtthuan3.ecommerce.catalogservice.contstant.SynonymSetStatus;
import com.dtthuan3.ecommerce.catalogservice.dto.request.SynonymSetCreateRequest;
import com.dtthuan3.ecommerce.catalogservice.dto.request.SynonymSetUpdateRequest;
import com.dtthuan3.ecommerce.catalogservice.dto.response.SynonymSetResponse;
import com.dtthuan3.ecommerce.catalogservice.entity.SynonymSet;
import com.dtthuan3.ecommerce.catalogservice.mapper.SynonymMapper;
import com.dtthuan3.ecommerce.catalogservice.repository.SynonymRepository;
import com.dtthuan3.ecommerce.catalogservice.repository.SynonymSetRepository;
import com.dtthuan3.ecommerce.catalogservice.service.CacheKeys;
import com.dtthuan3.ecommerce.catalogservice.service.RedisCacheService;
import com.dtthuan3.ecommerce.catalogservice.service.SynonymSetService;
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
public class SynonymSetServiceImpl
        implements SynonymSetService {

    private final SynonymSetRepository synonymSetRepository;

    private final SynonymRepository synonymRepository;

    private final SynonymMapper synonymMapper;

    private final RedisCacheService redisCacheService;

    @Override
    @Transactional
    public SynonymSetResponse create(
            SynonymSetCreateRequest request
    ) {

        String name = normalizeName(request.getName());

        if (synonymSetRepository.existsByNameIgnoreCase(name)) {
            throw new BusinessException(
                    ErrorCode.CONFLICT,
                    "Tên synonym set đã tồn tại"
            );
        }

        SynonymSet synonymSet =
                synonymMapper.toEntity(request);

        synonymSet.setName(name);

        if (synonymSet.getStatus() == null) {
            synonymSet.setStatus(
                    SynonymSetStatus.ACTIVE
            );
        }

        synonymSet.setVersion(1L);

        SynonymSet saved =
                synonymSetRepository.save(synonymSet);

        evictSynonymSetCaches(saved.getId());

        return synonymMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public SynonymSetResponse getById(
            String id
    ) {

        String cacheKey =
                CacheKeys.SYNONYM_SET + id;

        Optional<SynonymSetResponse> cached =
                redisCacheService.get(
                        cacheKey,
                        SynonymSetResponse.class
                );

        if (cached.isPresent()) {
            return cached.get();
        }

        SynonymSet synonymSet =
                findSynonymSet(id);

        SynonymSetResponse response =
                synonymMapper.toResponse(synonymSet);

        redisCacheService.set(
                cacheKey,
                response,
                CacheTtl.MEDIUM
        );

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<SynonymSetResponse> getSynonymSets(
            SynonymSetStatus status,
            int page,
            int size
    ) {

        validatePage(page, size);

        Pageable pageable =
                PageRequest.of(
                        page,
                        size,
                        Sort.by(
                                Sort.Order.asc("name"),
                                Sort.Order.asc("id")
                        )
                );

        Page<SynonymSet> result;

        if (status != null) {

            result =
                    synonymSetRepository.findByStatus(
                            status,
                            pageable
                    );

        } else {

            result =
                    synonymSetRepository.findAll(
                            pageable
                    );
        }

        List<SynonymSetResponse> content =
                result.getContent()
                        .stream()
                        .map(synonymMapper::toResponse)
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
    public SynonymSetResponse update(
            String id,
            SynonymSetUpdateRequest request
    ) {

        SynonymSet synonymSet =
                findSynonymSet(id);

        String name =
                normalizeName(request.getName());

        if (synonymSetRepository
                .existsByNameIgnoreCaseAndIdNot(
                        name,
                        id
                )) {

            throw new BusinessException(
                    ErrorCode.CONFLICT,
                    "Tên synonym set đã tồn tại"
            );
        }

        synonymSet.setName(name);

        if (request.getStatus() != null) {
            synonymSet.setStatus(
                    request.getStatus()
            );
        }

        incrementVersion(synonymSet);

        SynonymSet saved =
                synonymSetRepository.save(synonymSet);

        evictSynonymSetCaches(id);

        return synonymMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public SynonymSetResponse updateStatus(
            String id,
            SynonymSetStatus status
    ) {

        if (status == null) {
            throw new BusinessException(
                    ErrorCode.BAD_REQUEST,
                    "Status không được để trống"
            );
        }

        SynonymSet synonymSet =
                findSynonymSet(id);

        synonymSet.setStatus(status);

        incrementVersion(synonymSet);

        SynonymSet saved =
                synonymSetRepository.save(synonymSet);

        evictSynonymSetCaches(id);

        return synonymMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(
            String id
    ) {

        SynonymSet synonymSet =
                findSynonymSet(id);

        if (synonymSet.getStatus()
                != SynonymSetStatus.ARCHIVED) {

            throw new BusinessException(
                    ErrorCode.BAD_REQUEST,
                    "Chỉ được xóa Synonym Set ở trạng thái ARCHIVED"
            );
        }

        synonymRepository.deleteBySynonymSetId(id);

        synonymSetRepository.delete(
                synonymSet
        );

        evictSynonymSetCaches(id);
    }

    private SynonymSet findSynonymSet(
            String id
    ) {

        return synonymSetRepository
                .findById(id)
                .orElseThrow(
                        () -> new ResourceNotFoundException(
                                "Không tìm thấy Synonym Set: " + id
                        )
                );
    }

    private void incrementVersion(
            SynonymSet synonymSet
    ) {

        Long version =
                synonymSet.getVersion();

        if (version == null) {
            version = 0L;
        }

        synonymSet.setVersion(
                version + 1
        );
    }

    private String normalizeName(
            String name
    ) {

        if (name == null) {
            return null;
        }

        return name.trim();
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

    private void evictSynonymSetCaches(
            String synonymSetId
    ) {

        redisCacheService.delete(
                CacheKeys.SYNONYM_SET + synonymSetId
        );

        redisCacheService.delete(
                CacheKeys.SYNONYM_SET_LIST
        );

        redisCacheService.delete(
                CacheKeys.SYNONYM_SET_ACTIVE
        );

        redisCacheService.delete(
                CacheKeys.SYNONYM_LIST + synonymSetId
        );
    }
}
