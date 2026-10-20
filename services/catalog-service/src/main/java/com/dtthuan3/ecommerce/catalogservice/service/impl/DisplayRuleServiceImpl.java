package com.dtthuan3.ecommerce.catalogservice.service.impl;


import com.dtthuan3.ecommerce.catalogservice.contstant.CacheTtl;
import com.dtthuan3.ecommerce.catalogservice.contstant.DisplayRuleStatus;
import com.dtthuan3.ecommerce.catalogservice.contstant.DisplayRuleType;
import com.dtthuan3.ecommerce.catalogservice.dto.request.DisplayRuleCreateRequest;
import com.dtthuan3.ecommerce.catalogservice.dto.request.DisplayRuleUpdateRequest;
import com.dtthuan3.ecommerce.catalogservice.dto.response.DisplayRuleResponse;
import com.dtthuan3.ecommerce.catalogservice.entity.Collection;
import com.dtthuan3.ecommerce.catalogservice.entity.DisplayRule;
import com.dtthuan3.ecommerce.catalogservice.mapper.DisplayRuleMapper;
import com.dtthuan3.ecommerce.catalogservice.repository.CollectionRepository;
import com.dtthuan3.ecommerce.catalogservice.repository.DisplayRuleRepository;
import com.dtthuan3.ecommerce.catalogservice.service.CacheKeys;
import com.dtthuan3.ecommerce.catalogservice.service.RedisCacheService;
import com.dtthuan3.ecommerce.catalogservice.service.DisplayRuleService;
import com.dtthuan3.ecommerce.common.exception.BusinessException;
import com.dtthuan3.ecommerce.common.exception.ErrorCode;
import com.dtthuan3.ecommerce.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DisplayRuleServiceImpl
        implements DisplayRuleService {

    private final DisplayRuleRepository displayRuleRepository;

    private final CollectionRepository collectionRepository;

    private final DisplayRuleMapper displayRuleMapper;

    private final RedisCacheService redisCacheService;

    @Override
    @Transactional
    public DisplayRuleResponse create(
            String collectionId,
            DisplayRuleCreateRequest request
    ) {

        findCollection(collectionId);

        validateRuleConfig(
                request.ruleType(),
                request.ruleConfig()
        );

        Integer priority =
                request.priority() != null
                        ? request.priority()
                        : 0;

        if (displayRuleRepository
                .existsByCollectionIdAndPriority(
                        collectionId,
                        priority
                )) {

            throw new BusinessException(
                    ErrorCode.CONFLICT,
                    "Priority của Display Rule đã tồn tại trong Collection"
            );
        }

        DisplayRule rule =
                displayRuleMapper.toEntity(request);

        rule.setCollectionId(collectionId);
        rule.setPriority(priority);

        if (rule.getStatus() == null) {
            rule.setStatus(
                    DisplayRuleStatus.ACTIVE
            );
        }

        if (rule.getRevision() == null) {
            rule.setRevision(0L);
        }

        DisplayRule saved =
                displayRuleRepository.save(rule);

        evictCollectionCaches(collectionId);

        return displayRuleMapper.toResponse(saved);
    }

    @Override
    public DisplayRuleResponse getById(
            String id
    ) {

        String cacheKey =
                CacheKeys.DISPLAY_RULE + id;

        Optional<DisplayRuleResponse> cached =
                redisCacheService.get(
                        cacheKey,
                        DisplayRuleResponse.class
                );

        if (cached.isPresent()) {
            return cached.get();
        }

        DisplayRule rule =
                findRule(id);

        DisplayRuleResponse response =
                displayRuleMapper.toResponse(rule);

        redisCacheService.set(
                cacheKey,
                response,
                CacheTtl.MEDIUM
        );

        return response;
    }

    @Override
    public List<DisplayRuleResponse> getByCollectionId(
            String collectionId
    ) {

        findCollection(collectionId);

        String cacheKey =
                CacheKeys.DISPLAY_RULE_COLLECTION + collectionId;

        List<DisplayRuleResponse> cached =
                redisCacheService.getList(
                        cacheKey,
                        DisplayRuleResponse.class
                );

        if (!cached.isEmpty()
                || redisCacheService.hasKey(cacheKey)) {

            return cached;
        }

        List<DisplayRuleResponse> result =
                displayRuleRepository
                        .findByCollectionIdOrderByPriorityAscIdAsc(
                                collectionId
                        )
                        .stream()
                        .map(displayRuleMapper::toResponse)
                        .toList();

        redisCacheService.set(
                cacheKey,
                result,
                CacheTtl.MEDIUM
        );

        return result;
    }

    @Override
    public List<DisplayRuleResponse> getActiveByCollectionId(
            String collectionId
    ) {

        findCollection(collectionId);

        String cacheKey =
                CacheKeys.DISPLAY_RULE_ACTIVE + collectionId;

        List<DisplayRuleResponse> cached =
                redisCacheService.getList(
                        cacheKey,
                        DisplayRuleResponse.class
                );

        if (!cached.isEmpty()
                || redisCacheService.hasKey(cacheKey)) {

            return cached;
        }

        List<DisplayRuleResponse> result =
                displayRuleRepository
                        .findByCollectionIdAndStatusOrderByPriorityAscIdAsc(
                                collectionId,
                                DisplayRuleStatus.ACTIVE
                        )
                        .stream()
                        .map(displayRuleMapper::toResponse)
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
    public DisplayRuleResponse update(
            String id,
            DisplayRuleUpdateRequest request
    ) {

        DisplayRule rule =
                findRule(id);

        if (request.ruleType() != null) {

            Map<String, Object> config =
                    request.ruleConfig() != null
                            ? request.ruleConfig()
                            : rule.getRuleConfig();

            validateRuleConfig(
                    request.ruleType(),
                    config
            );

            rule.setRuleType(
                    request.ruleType()
            );

            if (request.ruleConfig() != null) {
                rule.setRuleConfig(
                        request.ruleConfig()
                );
            }

        } else if (request.ruleConfig() != null) {

            validateRuleConfig(
                    rule.getRuleType(),
                    request.ruleConfig()
            );

            rule.setRuleConfig(
                    request.ruleConfig()
            );
        }

        if (request.priority() != null) {

            if (displayRuleRepository
                    .existsByCollectionIdAndPriorityAndIdNot(
                            rule.getCollectionId(),
                            request.priority(),
                            id
                    )) {

                throw new BusinessException(
                        ErrorCode.CONFLICT,
                        "Priority của Display Rule đã tồn tại trong Collection"
                );
            }

            rule.setPriority(
                    request.priority()
            );
        }

        if (request.status() != null) {
            rule.setStatus(
                    request.status()
            );
        }

        incrementRevision(rule);

        DisplayRule saved =
                displayRuleRepository.save(rule);

        evictRuleCaches(
                saved.getId(),
                saved.getCollectionId()
        );

        return displayRuleMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public DisplayRuleResponse updateStatus(
            String id,
            DisplayRuleStatus status
    ) {

        if (status == null) {
            throw new BusinessException(
                    ErrorCode.BAD_REQUEST,
                    "Status không được để trống"
            );
        }

        DisplayRule rule =
                findRule(id);

        rule.setStatus(status);

        incrementRevision(rule);

        DisplayRule saved =
                displayRuleRepository.save(rule);

        evictRuleCaches(
                saved.getId(),
                saved.getCollectionId()
        );

        return displayRuleMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(
            String id
    ) {

        DisplayRule rule =
                findRule(id);

        String collectionId =
                rule.getCollectionId();

        displayRuleRepository.delete(rule);

        evictRuleCaches(
                id,
                collectionId
        );
    }

    private DisplayRule findRule(
            String id
    ) {

        return displayRuleRepository
                .findById(id)
                .orElseThrow(
                        () -> new ResourceNotFoundException(
                                "Không tìm thấy Display Rule: " + id
                        )
                );
    }

    private Collection findCollection(
            String collectionId
    ) {

        return collectionRepository
                .findById(collectionId)
                .orElseThrow(
                        () -> new ResourceNotFoundException(
                                "Không tìm thấy Collection: "
                                        + collectionId
                        )
                );
    }

    private void incrementRevision(
            DisplayRule rule
    ) {

        Long revision =
                rule.getRevision();

        if (revision == null) {
            revision = 0L;
        }

        rule.setRevision(
                revision + 1
        );
    }

    private void evictRuleCaches(
            String ruleId,
            String collectionId
    ) {

        redisCacheService.delete(
                CacheKeys.DISPLAY_RULE + ruleId
        );

        evictCollectionCaches(
                collectionId
        );
    }

    private void evictCollectionCaches(
            String collectionId
    ) {

        redisCacheService.delete(
                CacheKeys.DISPLAY_RULE_COLLECTION + collectionId
        );

        redisCacheService.delete(
                CacheKeys.DISPLAY_RULE_ACTIVE + collectionId
        );
    }

    private void validateRuleConfig(
            DisplayRuleType ruleType,
            Map<String, Object> config
    ) {

        if (ruleType == null) {
            throw new BusinessException(
                    ErrorCode.BAD_REQUEST,
                    "Rule type không được để trống"
            );
        }

        if (config == null || config.isEmpty()) {
            throw new BusinessException(
                    ErrorCode.BAD_REQUEST,
                    "Rule config không được để trống"
            );
        }

        switch (ruleType) {

            case CATEGORY -> {

                Object categoryIds =
                        config.get("categoryIds");

                if (!(categoryIds instanceof List<?> list)
                        || list.isEmpty()) {

                    throw new BusinessException(
                            ErrorCode.BAD_REQUEST,
                            "CATEGORY rule phải có categoryIds"
                    );
                }
            }

            case BRAND -> {

                Object brandIds =
                        config.get("brandIds");

                if (!(brandIds instanceof List<?> list)
                        || list.isEmpty()) {

                    throw new BusinessException(
                            ErrorCode.BAD_REQUEST,
                            "BRAND rule phải có brandIds"
                    );
                }
            }

            case PRICE_RANGE -> {

                Object minPrice =
                        config.get("minPrice");

                Object maxPrice =
                        config.get("maxPrice");

                if (minPrice == null && maxPrice == null) {

                    throw new BusinessException(
                            ErrorCode.BAD_REQUEST,
                            "PRICE_RANGE phải có minPrice hoặc maxPrice"
                    );
                }

                if (minPrice != null
                        && maxPrice != null
                        && toDouble(minPrice)
                        > toDouble(maxPrice)) {

                    throw new BusinessException(
                            ErrorCode.BAD_REQUEST,
                            "minPrice không được lớn hơn maxPrice"
                    );
                }
            }

            case RATING -> {

                Object minRating =
                        config.get("minRating");

                if (minRating == null) {

                    throw new BusinessException(
                            ErrorCode.BAD_REQUEST,
                            "RATING rule phải có minRating"
                    );
                }

                double rating =
                        toDouble(minRating);

                if (rating < 0 || rating > 5) {

                    throw new BusinessException(
                            ErrorCode.BAD_REQUEST,
                            "minRating phải nằm trong khoảng 0 đến 5"
                    );
                }
            }

            case BUYABLE -> {

                Object buyable =
                        config.get("buyable");

                if (!(buyable instanceof Boolean)) {

                    throw new BusinessException(
                            ErrorCode.BAD_REQUEST,
                            "BUYABLE rule phải có buyable kiểu boolean"
                    );
                }
            }
        }
    }

    private double toDouble(
            Object value
    ) {

        if (value instanceof Number number) {
            return number.doubleValue();
        }

        try {
            return Double.parseDouble(
                    value.toString()
            );
        } catch (NumberFormatException ex) {

            throw new BusinessException(
                    ErrorCode.BAD_REQUEST,
                    "Giá trị rule không hợp lệ: " + value
            );
        }
    }
}