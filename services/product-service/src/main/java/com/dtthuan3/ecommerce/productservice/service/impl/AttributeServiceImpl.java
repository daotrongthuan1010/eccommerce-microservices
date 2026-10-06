package com.dtthuan3.ecommerce.productservice.service.impl;

import com.dtthuan3.ecommerce.productservice.dto.request.AttributeOptionRequest;
import com.dtthuan3.ecommerce.productservice.dto.request.AttributeRequest;
import com.dtthuan3.ecommerce.productservice.dto.response.AttributeOptionResponse;
import com.dtthuan3.ecommerce.productservice.dto.response.AttributeResponse;
import com.dtthuan3.ecommerce.productservice.domain.Attribute;
import com.dtthuan3.ecommerce.productservice.domain.AttributeOption;
import com.dtthuan3.ecommerce.productservice.exception.DuplicateResourceException;
import com.dtthuan3.ecommerce.productservice.exception.ResourceNotFoundException;
import com.dtthuan3.ecommerce.productservice.mapper.AttributeMapper;
import com.dtthuan3.ecommerce.productservice.mapper.AttributeOptionMapper;
import com.dtthuan3.ecommerce.productservice.repository.AttributeOptionRepository;
import com.dtthuan3.ecommerce.productservice.repository.AttributeRepository;
import com.dtthuan3.ecommerce.productservice.service.AttributeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AttributeServiceImpl implements AttributeService {

    private final AttributeRepository attributeRepository;
    private final AttributeOptionRepository attributeOptionRepository;

    private final AttributeMapper attributeMapper;
    private final AttributeOptionMapper attributeOptionMapper;

    @Override
    @Transactional
    public AttributeResponse create(AttributeRequest request) {

        if (attributeRepository.existsByCode(request.getCode())) {
            throw new DuplicateResourceException(
                    "Attribute code đã tồn tại: " + request.getCode()
            );
        }

        Attribute attribute = attributeMapper.toEntity(request);

        Attribute saved = attributeRepository.save(attribute);

        return attributeMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public AttributeResponse getById(Long id) {

        Attribute attribute = attributeRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Không tìm thấy attribute với id: " + id
                        )
                );

        return attributeMapper.toResponse(attribute);
    }

    @Override
    @Transactional
    public AttributeResponse update(
            Long id,
            AttributeRequest request
    ) {

        Attribute attribute = attributeRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Không tìm thấy attribute với id: " + id
                        )
                );

        if (attributeRepository.existsByCodeAndIdNot(
                request.getCode(),
                id
        )) {
            throw new DuplicateResourceException(
                    "Attribute code đã tồn tại: " + request.getCode()
            );
        }

        attributeMapper.updateEntity(request, attribute);

        Attribute updated = attributeRepository.save(attribute);

        return attributeMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public void delete(Long id) {

        Attribute attribute = attributeRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Không tìm thấy attribute với id: " + id
                        )
                );

        attributeRepository.delete(attribute);
    }

    // =========================
    // Attribute Option
    // =========================

    @Override
    @Transactional
    public AttributeOptionResponse createOption(
            AttributeOptionRequest request
    ) {

        if (!attributeRepository.existsById(request.getAttributeId())) {
            throw new ResourceNotFoundException(
                    "Không tìm thấy attribute với id: "
                            + request.getAttributeId()
            );
        }

        if (attributeOptionRepository
                .existsByAttributeIdAndValue(
                        request.getAttributeId(),
                        request.getValue()
                )) {

            throw new DuplicateResourceException(
                    "Option đã tồn tại trong attribute"
            );
        }

        AttributeOption option =
                attributeOptionMapper.toEntity(request);

        AttributeOption saved =
                attributeOptionRepository.save(option);

        return attributeOptionMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public AttributeOptionResponse getOptionById(Long id) {

        AttributeOption option =
                attributeOptionRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Không tìm thấy attribute option với id: "
                                                + id
                                )
                        );

        return attributeOptionMapper.toResponse(option);
    }

    @Override
    @Transactional
    public AttributeOptionResponse updateOption(
            Long id,
            AttributeOptionRequest request
    ) {

        AttributeOption option =
                attributeOptionRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Không tìm thấy attribute option với id: "
                                                + id
                                )
                        );

        if (!attributeRepository.existsById(request.getAttributeId())) {
            throw new ResourceNotFoundException(
                    "Không tìm thấy attribute với id: "
                            + request.getAttributeId()
            );
        }

        if (attributeOptionRepository
                .existsByAttributeIdAndValueAndIdNot(
                        request.getAttributeId(),
                        request.getValue(),
                        id
                )) {

            throw new DuplicateResourceException(
                    "Option đã tồn tại trong attribute"
            );
        }

        attributeOptionMapper.updateEntity(request, option);

        AttributeOption updated =
                attributeOptionRepository.save(option);

        return attributeOptionMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public void deleteOption(Long id) {

        AttributeOption option =
                attributeOptionRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Không tìm thấy attribute option với id: "
                                                + id
                                )
                        );

        attributeOptionRepository.delete(option);
    }
}