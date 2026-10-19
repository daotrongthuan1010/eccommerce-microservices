package com.dtthuan3.ecommerce.productservice.service.impl;

import com.dtthuan3.ecommerce.common.exception.BusinessException;
import com.dtthuan3.ecommerce.common.exception.ErrorCode;
import com.dtthuan3.ecommerce.common.exception.ResourceNotFoundException;
import com.dtthuan3.ecommerce.productservice.domain.Attribute;
import com.dtthuan3.ecommerce.productservice.domain.AttributeTemplate;
import com.dtthuan3.ecommerce.productservice.domain.AttributeTemplateItem;
import com.dtthuan3.ecommerce.productservice.dto.request.AttributeTemplateItemCreateRequest;
import com.dtthuan3.ecommerce.productservice.dto.request.AttributeTemplateItemUpdateRequest;
import com.dtthuan3.ecommerce.productservice.dto.request.AttributeTemplateRequest;
import com.dtthuan3.ecommerce.productservice.dto.response.AttributeTemplateItemResponse;
import com.dtthuan3.ecommerce.productservice.dto.response.AttributeTemplateResponse;
import com.dtthuan3.ecommerce.productservice.mapper.AttributeTemplateItemMapper;
import com.dtthuan3.ecommerce.productservice.mapper.AttributeTemplateMapper;
import com.dtthuan3.ecommerce.productservice.repository.AttributeRepository;
import com.dtthuan3.ecommerce.productservice.repository.AttributeTemplateItemRepository;
import com.dtthuan3.ecommerce.productservice.repository.AttributeTemplateRepository;
import com.dtthuan3.ecommerce.productservice.service.AttributeTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AttributeTemplateServiceImpl implements AttributeTemplateService {

    private final AttributeTemplateRepository templateRepository;
    private final AttributeTemplateItemRepository itemRepository;
    private final AttributeRepository attributeRepository;

    private final AttributeTemplateMapper templateMapper;
    private final AttributeTemplateItemMapper itemMapper;

    // =========================
    // ATTRIBUTE TEMPLATE
    // =========================

    @Override
    @Transactional
    public AttributeTemplateResponse create(AttributeTemplateRequest request) {
        String code = normalizeCode(request.getCode());

        if (templateRepository.existsByCodeIgnoreCase(code)) {
            throw new BusinessException(
                    ErrorCode.CONFLICT,
                    "Mã template đã tồn tại: " + code
            );
        }

        AttributeTemplate template = AttributeTemplate.builder()
                .name(request.getName().trim())
                .code(code)
                .description(normalizeText(request.getDescription()))
                .active(true)
                .build();

        AttributeTemplate saved = templateRepository.save(template);

        return toTemplateResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public AttributeTemplateResponse getById(UUID templateId) {
        return toTemplateResponse(getActiveTemplate(templateId));
    }

    @Override
    @Transactional
    public AttributeTemplateResponse update(
            UUID templateId,
            AttributeTemplateRequest request
    ) {
        AttributeTemplate template = getActiveTemplate(templateId);
        String code = normalizeCode(request.getCode());

        if (templateRepository.existsByCodeIgnoreCaseAndIdNot(
                code,
                templateId
        )) {
            throw new BusinessException(
                    ErrorCode.CONFLICT,
                    "Mã template đã được sử dụng: " + code
            );
        }

        template.setName(request.getName().trim());
        template.setCode(code);
        template.setDescription(normalizeText(request.getDescription()));

        AttributeTemplate saved = templateRepository.save(template);

        return toTemplateResponse(saved);
    }

    @Override
    @Transactional
    public void deactivate(UUID templateId) {
        AttributeTemplate template = getActiveTemplate(templateId);

        template.setActive(false);

        templateRepository.save(template);
    }

    // =========================
    // ATTRIBUTE TEMPLATE ITEM
    // =========================

    @Override
    @Transactional
    public AttributeTemplateItemResponse createItem(
            UUID templateId,
            AttributeTemplateItemCreateRequest request
    ) {
        AttributeTemplate template = getActiveTemplate(templateId);
        Attribute attribute = getActiveAttribute(request.attributeId());

        var existing = itemRepository.findByTemplateIdAndAttributeId(
                template.getId(),
                attribute.getId()
        );

        if (existing.isPresent()) {
            AttributeTemplateItem item = existing.get();

            if (Boolean.TRUE.equals(item.getActive())) {
                throw new BusinessException(
                        ErrorCode.CONFLICT,
                        "Thuộc tính đã tồn tại trong template"
                );
            }

            applyCreateRequest(item, request);
            item.setActive(true);

            return itemMapper.toResponse(itemRepository.save(item));
        }

        AttributeTemplateItem item = AttributeTemplateItem.builder()
                .templateId(template.getId())
                .attributeId(attribute.getId())
                .variantAttribute(Boolean.TRUE.equals(request.variantAttribute()))
                .displayOrder(request.displayOrder())
                .required(Boolean.TRUE.equals(request.required()))
                .active(true)
                .build();

        return itemMapper.toResponse(itemRepository.save(item));
    }

    @Override
    @Transactional(readOnly = true)
    public AttributeTemplateItemResponse getItemById(UUID itemId) {
        return itemMapper.toResponse(getActiveItem(itemId));
    }

    @Override
    @Transactional
    public AttributeTemplateItemResponse updateItem(
            UUID itemId,
            AttributeTemplateItemUpdateRequest request
    ) {
        AttributeTemplateItem item = getActiveItem(itemId);

        if (request.variantAttribute() != null) {
            item.setVariantAttribute(request.variantAttribute());
        }

        if (request.displayOrder() != null) {
            item.setDisplayOrder(request.displayOrder());
        }

        if (request.required() != null) {
            item.setRequired(request.required());
        }

        return itemMapper.toResponse(itemRepository.save(item));
    }

    @Override
    @Transactional
    public void deactivateItem(UUID itemId) {
        AttributeTemplateItem item = getActiveItem(itemId);

        item.setActive(false);

        itemRepository.save(item);
    }

    // =========================
    // PRIVATE METHODS
    // =========================

    private AttributeTemplateResponse toTemplateResponse(
            AttributeTemplate template
    ) {
        List<AttributeTemplateItemResponse> items = itemRepository
                .findAllByTemplateIdAndActiveTrueOrderByDisplayOrderAsc(
                        template.getId()
                )
                .stream()
                .map(itemMapper::toResponse)
                .toList();

        AttributeTemplateResponse response = templateMapper.toResponse(template);

        return new AttributeTemplateResponse(
                response.id(),
                response.name(),
                response.code(),
                response.description(),
                response.active(),
                response.createdAt(),
                response.updatedAt(),
                items
        );
    }

    private AttributeTemplate getActiveTemplate(UUID templateId) {
        return templateRepository.findByIdAndActiveTrue(templateId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "AttributeTemplate",
                                templateId
                        )
                );
    }

    private Attribute getActiveAttribute(UUID attributeId) {
        return attributeRepository.findByIdAndActiveTrue(attributeId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Attribute",
                                attributeId
                        )
                );
    }

    private AttributeTemplateItem getActiveItem(UUID itemId) {
        AttributeTemplateItem item = itemRepository.findById(itemId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "AttributeTemplateItem",
                                itemId
                        )
                );

        if (!Boolean.TRUE.equals(item.getActive())) {
            throw new ResourceNotFoundException(
                    "AttributeTemplateItem",
                    itemId
            );
        }

        return item;
    }

    private void applyCreateRequest(
            AttributeTemplateItem item,
            AttributeTemplateItemCreateRequest request
    ) {
        item.setVariantAttribute(
                Boolean.TRUE.equals(request.variantAttribute())
        );
        item.setDisplayOrder(request.displayOrder());
        item.setRequired(Boolean.TRUE.equals(request.required()));
    }

    private String normalizeCode(String code) {
        if (code == null || code.isBlank()) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_ERROR,
                    "Mã template không được để trống"
            );
        }

        return code.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeText(String value) {
        return value == null || value.isBlank()
                ? null
                : value.trim();
    }
}