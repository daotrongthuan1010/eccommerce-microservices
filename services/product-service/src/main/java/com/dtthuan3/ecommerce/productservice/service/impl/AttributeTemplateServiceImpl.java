package com.dtthuan3.ecommerce.productservice.service.impl;

import com.dtthuan3.ecommerce.productservice.dto.request.AttributeTemplateItemRequest;
import com.dtthuan3.ecommerce.productservice.dto.request.AttributeTemplateRequest;
import com.dtthuan3.ecommerce.productservice.dto.response.AttributeTemplateItemResponse;
import com.dtthuan3.ecommerce.productservice.dto.response.AttributeTemplateResponse;
import com.dtthuan3.ecommerce.productservice.domain.AttributeTemplate;
import com.dtthuan3.ecommerce.productservice.domain.AttributeTemplateItem;
import com.dtthuan3.ecommerce.productservice.exception.DuplicateResourceException;
import com.dtthuan3.ecommerce.productservice.exception.ResourceNotFoundException;
import com.dtthuan3.ecommerce.productservice.mapper.AttributeTemplateItemMapper;
import com.dtthuan3.ecommerce.productservice.mapper.AttributeTemplateMapper;
import com.dtthuan3.ecommerce.productservice.repository.AttributeTemplateItemRepository;
import com.dtthuan3.ecommerce.productservice.repository.AttributeTemplateRepository;
import com.dtthuan3.ecommerce.productservice.service.AttributeTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AttributeTemplateServiceImpl
        implements AttributeTemplateService {

    private final AttributeTemplateRepository attributeTemplateRepository;
    private final AttributeTemplateItemRepository attributeTemplateItemRepository;

    private final AttributeTemplateMapper attributeTemplateMapper;
    private final AttributeTemplateItemMapper attributeTemplateItemMapper;

    @Override
    @Transactional
    public AttributeTemplateResponse create(
            AttributeTemplateRequest request
    ) {

        if (attributeTemplateRepository.existsByCode(
                request.getCode()
        )) {
            throw new DuplicateResourceException(
                    "Attribute template code đã tồn tại: "
                            + request.getCode()
            );
        }

        AttributeTemplate template =
                attributeTemplateMapper.toEntity(request);

        AttributeTemplate saved =
                attributeTemplateRepository.save(template);

        return attributeTemplateMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public AttributeTemplateResponse getById(Long id) {

        AttributeTemplate template =
                attributeTemplateRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Không tìm thấy attribute template với id: "
                                                + id
                                )
                        );

        return attributeTemplateMapper.toResponse(template);
    }

    @Override
    @Transactional
    public AttributeTemplateResponse update(
            Long id,
            AttributeTemplateRequest request
    ) {

        AttributeTemplate template =
                attributeTemplateRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Không tìm thấy attribute template với id: "
                                                + id
                                )
                        );

        if (attributeTemplateRepository.existsByCodeAndIdNot(
                request.getCode(),
                id
        )) {
            throw new DuplicateResourceException(
                    "Attribute template code đã tồn tại: "
                            + request.getCode()
            );
        }

        attributeTemplateMapper.updateEntity(request, template);

        AttributeTemplate updated =
                attributeTemplateRepository.save(template);

        return attributeTemplateMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public void delete(Long id) {

        AttributeTemplate template =
                attributeTemplateRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Không tìm thấy attribute template với id: "
                                                + id
                                )
                        );

        attributeTemplateRepository.delete(template);
    }

    // =========================
    // Attribute Template Item
    // =========================

    @Override
    @Transactional
    public AttributeTemplateItemResponse createItem(
            AttributeTemplateItemRequest request
    ) {

        if (!attributeTemplateRepository.existsById(
                request.getTemplateId()
        )) {
            throw new ResourceNotFoundException(
                    "Không tìm thấy attribute template với id: "
                            + request.getTemplateId()
            );
        }

        if (attributeTemplateItemRepository
                .existsByTemplateIdAndAttributeId(
                        request.getTemplateId(),
                        request.getAttributeId()
                )) {

            throw new DuplicateResourceException(
                    "Attribute đã tồn tại trong template"
            );
        }

        AttributeTemplateItem item =
                attributeTemplateItemMapper.toEntity(request);

        AttributeTemplateItem saved =
                attributeTemplateItemRepository.save(item);

        return attributeTemplateItemMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public AttributeTemplateItemResponse getItemById(
            Long id
    ) {

        AttributeTemplateItem item =
                attributeTemplateItemRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Không tìm thấy template item với id: "
                                                + id
                                )
                        );

        return attributeTemplateItemMapper.toResponse(item);
    }

    @Override
    @Transactional
    public AttributeTemplateItemResponse updateItem(
            Long id,
            AttributeTemplateItemRequest request
    ) {

        AttributeTemplateItem item =
                attributeTemplateItemRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Không tìm thấy template item với id: "
                                                + id
                                )
                        );

        if (!attributeTemplateRepository.existsById(
                request.getTemplateId()
        )) {
            throw new ResourceNotFoundException(
                    "Không tìm thấy attribute template với id: "
                            + request.getTemplateId()
            );
        }

        if (attributeTemplateItemRepository
                .existsByTemplateIdAndAttributeIdAndIdNot(
                        request.getTemplateId(),
                        request.getAttributeId(),
                        id
                )) {

            throw new DuplicateResourceException(
                    "Attribute đã tồn tại trong template"
            );
        }

        attributeTemplateItemMapper.updateEntity(request, item);

        AttributeTemplateItem updated =
                attributeTemplateItemRepository.save(item);

        return attributeTemplateItemMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public void deleteItem(Long id) {

        AttributeTemplateItem item =
                attributeTemplateItemRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Không tìm thấy template item với id: "
                                                + id
                                )
                        );

        attributeTemplateItemRepository.delete(item);
    }
}