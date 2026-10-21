package com.dtthuan3.ecommerce.catalogservice.service.impl;

import com.dtthuan3.ecommerce.catalogservice.contstant.CacheTtl;
import com.dtthuan3.ecommerce.catalogservice.dto.request.SynonymCreateRequest;
import com.dtthuan3.ecommerce.catalogservice.dto.request.SynonymUpdateRequest;
import com.dtthuan3.ecommerce.catalogservice.dto.response.SynonymResponse;
import com.dtthuan3.ecommerce.catalogservice.entity.Synonym;
import com.dtthuan3.ecommerce.catalogservice.entity.SynonymSet;
import com.dtthuan3.ecommerce.catalogservice.mapper.SynonymMapper;
import com.dtthuan3.ecommerce.catalogservice.repository.SynonymRepository;
import com.dtthuan3.ecommerce.catalogservice.repository.SynonymSetRepository;
import com.dtthuan3.ecommerce.catalogservice.service.CacheKeys;
import com.dtthuan3.ecommerce.catalogservice.service.RedisCacheService;
import com.dtthuan3.ecommerce.catalogservice.service.SynonymService;
import com.dtthuan3.ecommerce.common.exception.BusinessException;
import com.dtthuan3.ecommerce.common.exception.ErrorCode;
import com.dtthuan3.ecommerce.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SynonymServiceImpl
        implements SynonymService {

    private final SynonymRepository synonymRepository;

    private final SynonymSetRepository synonymSetRepository;

    private final SynonymMapper synonymMapper;

    private final RedisCacheService redisCacheService;

    @Override
    @Transactional
    public SynonymResponse create(
            String synonymSetId,
            SynonymCreateRequest request
    ) {

        SynonymSet synonymSet =
                findSynonymSet(synonymSetId);

        validateSynonymSetStatus(synonymSet);

        String term =
                normalize(request.getTerm());

        String synonym =
                normalize(request.getSynonym());

        if (synonymRepository
                .existsBySynonymSetIdAndTermAndSynonym(
                        synonymSetId,
                        term,
                        synonym
                )) {

            throw new BusinessException(
                    ErrorCode.CONFLICT,
                    "Synonym đã tồn tại trong Synonym Set"
            );
        }

        Synonym entity =
                synonymMapper.toEntity(request);

        entity.setSynonymSetId(
                synonymSetId
        );

        entity.setTerm(term);
        entity.setSynonym(synonym);

        Synonym saved =
                synonymRepository.save(entity);

        incrementVersion(synonymSet);

        synonymSetRepository.save(
                synonymSet
        );

        evictCaches(synonymSetId, saved.getId());

        return synonymMapper.toResponse(saved);
    }

    @Override
    public SynonymResponse getById(
            String id
    ) {

        String cacheKey =
                CacheKeys.SYNONYM + id;

        Optional<SynonymResponse> cached =
                redisCacheService.get(
                        cacheKey,
                        SynonymResponse.class
                );

        if (cached.isPresent()) {
            return cached.get();
        }

        Synonym synonym =
                findSynonym(id);

        SynonymResponse response =
                synonymMapper.toResponse(synonym);

        redisCacheService.set(
                cacheKey,
                response,
                CacheTtl.MEDIUM
        );

        return response;
    }

    @Override
    public List<SynonymResponse> getBySynonymSetId(
            String synonymSetId
    ) {

        findSynonymSet(synonymSetId);

        String cacheKey =
                CacheKeys.SYNONYM_LIST
                        + synonymSetId;

        List<SynonymResponse> cached =
                redisCacheService.getList(
                        cacheKey,
                        SynonymResponse.class
                );

        if (!cached.isEmpty()
                || redisCacheService.hasKey(cacheKey)) {

            return cached;
        }

        List<SynonymResponse> result =
                synonymRepository
                        .findBySynonymSetIdOrderByTermAscSynonymAsc(
                                synonymSetId
                        )
                        .stream()
                        .map(synonymMapper::toResponse)
                        .toList();

        redisCacheService.set(
                cacheKey,
                result,
                CacheTtl.MEDIUM
        );

        return result;
    }

    @Override
    @Transactional
    public SynonymResponse update(
            String id,
            SynonymUpdateRequest request
    ) {

        Synonym synonym =
                findSynonym(id);

        SynonymSet synonymSet =
                findSynonymSet(
                        synonym.getSynonymSetId()
                );

        validateSynonymSetStatus(synonymSet);

        String term =
                normalize(request.getTerm());

        String synonymValue =
                normalize(request.getSynonym());

        if (synonymRepository
                .existsBySynonymSetIdAndTermAndSynonymAndIdNot(
                        synonym.getSynonymSetId(),
                        term,
                        synonymValue,
                        id
                )) {

            throw new BusinessException(
                    ErrorCode.CONFLICT,
                    "Synonym đã tồn tại trong Synonym Set"
            );
        }

        synonym.setTerm(term);
        synonym.setSynonym(synonymValue);

        Synonym saved =
                synonymRepository.save(synonym);

        incrementVersion(synonymSet);

        synonymSetRepository.save(
                synonymSet
        );

        evictCaches(
                synonym.getSynonymSetId(),
                synonym.getId()
        );

        return synonymMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(
            String id
    ) {

        Synonym synonym =
                findSynonym(id);

        SynonymSet synonymSet =
                findSynonymSet(
                        synonym.getSynonymSetId()
                );

        validateSynonymSetStatus(synonymSet);

        synonymRepository.delete(
                synonym
        );

        incrementVersion(synonymSet);

        synonymSetRepository.save(
                synonymSet
        );

        evictCaches(
                synonym.getSynonymSetId(),
                id
        );
    }

    private Synonym findSynonym(
            String id
    ) {

        return synonymRepository
                .findById(id)
                .orElseThrow(
                        () -> new ResourceNotFoundException(
                                "Không tìm thấy Synonym: " + id
                        )
                );
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

    private void validateSynonymSetStatus(
            SynonymSet synonymSet
    ) {

        if (synonymSet.getStatus()
                == com.dtthuan3.ecommerce.catalogservice.contstant.SynonymSetStatus.ARCHIVED) {

            throw new BusinessException(
                    ErrorCode.BAD_REQUEST,
                    "Synonym Set đã ARCHIVED, không thể thay đổi Synonym"
            );
        }
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

    private String normalize(
            String value
    ) {

        if (value == null) {
            return null;
        }

        return value.trim()
                .toLowerCase();
    }

    private void evictCaches(
            String synonymSetId,
            String synonymId
    ) {

        redisCacheService.delete(
                CacheKeys.SYNONYM + synonymId
        );

        redisCacheService.delete(
                CacheKeys.SYNONYM_LIST + synonymSetId
        );

        redisCacheService.delete(
                CacheKeys.SYNONYM_SET + synonymSetId
        );

        redisCacheService.delete(
                CacheKeys.SYNONYM_SET_LIST
        );

        redisCacheService.delete(
                CacheKeys.SYNONYM_SET_ACTIVE
        );
    }
}
