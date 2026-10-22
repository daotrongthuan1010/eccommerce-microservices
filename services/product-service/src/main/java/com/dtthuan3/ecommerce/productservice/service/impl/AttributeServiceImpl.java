
package com.dtthuan3.ecommerce.productservice.service.impl;

import com.dtthuan3.ecommerce.common.exception.BusinessException;
import com.dtthuan3.ecommerce.common.exception.ErrorCode;
import com.dtthuan3.ecommerce.common.exception.ResourceNotFoundException;
import com.dtthuan3.ecommerce.productservice.constant.AttributeDataType;
import com.dtthuan3.ecommerce.productservice.domain.Attribute;
import com.dtthuan3.ecommerce.productservice.dto.request.AttributeCreateRequest;
import com.dtthuan3.ecommerce.productservice.dto.request.AttributeUpdateRequest;
import com.dtthuan3.ecommerce.productservice.dto.response.AttributeResponse;
import com.dtthuan3.ecommerce.productservice.mapper.AttributeMapper;
import com.dtthuan3.ecommerce.productservice.repository.AttributeOptionRepository;
import com.dtthuan3.ecommerce.productservice.repository.AttributeRepository;
import com.dtthuan3.ecommerce.productservice.repository.ProductAttributeValueRepository;
import com.dtthuan3.ecommerce.productservice.repository.VariantAttributeValueRepository;
import com.dtthuan3.ecommerce.productservice.service.AttributeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AttributeServiceImpl implements AttributeService {

    private final AttributeRepository attributeRepository;
    private final AttributeOptionRepository attributeOptionRepository;
    private final ProductAttributeValueRepository productAttributeValueRepository;
    private final VariantAttributeValueRepository variantAttributeValueRepository;
    private final AttributeMapper attributeMapper;

    @Override
    @Transactional
    public AttributeResponse create(AttributeCreateRequest request) {
        String code = normalizeCode(request.code());

        if (attributeRepository.existsByCodeIgnoreCase(code)) {
            throw new BusinessException(
                    ErrorCode.CONFLICT,
                    "Mã thuộc tính đã tồn tại"
            );
        }

        Attribute attribute = Attribute.builder()
                .name(request.name().trim())
                .code(code)
                .dataType(request.dataType())
                .unit(normalizeUnit(request.unit()))
                .required(Boolean.TRUE.equals(request.required()))
                .active(true)
                .build();

        return attributeMapper.toResponse(
                attributeRepository.save(attribute)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public AttributeResponse getById(UUID attributeId) {
        return attributeMapper.toResponse(getActiveAttribute(attributeId));
    }

    @Override
    @Transactional
    public AttributeResponse update(
            UUID attributeId,
            AttributeUpdateRequest request
    ) {
        Attribute attribute = getActiveAttribute(attributeId);
        String code = normalizeCode(request.code());

        if (attributeRepository.existsByCodeIgnoreCaseAndIdNot(
                code,
                attributeId
        )) {
            throw new BusinessException(
                    ErrorCode.CONFLICT,
                    "Mã thuộc tính đã được sử dụng"
            );
        }

        if (attribute.getDataType() != request.dataType()
                && hasStoredValues(attributeId)) {
            throw new BusinessException(
                    ErrorCode.CONFLICT,
                    "Không thể đổi kiểu dữ liệu vì thuộc tính đã được sử dụng. "
                            + "Hãy tạo thuộc tính mới và chuyển đổi dữ liệu có kiểm soát."
            );
        }

        if (attribute.getDataType() != request.dataType()
                && request.dataType() != AttributeDataType.SELECT
                && attributeOptionRepository
                .findByAttributeIdAndActiveTrueOrderByDisplayOrderAscValueAsc(
                        attributeId
                ).size() > 0) {
            throw new BusinessException(
                    ErrorCode.CONFLICT,
                    "Không thể chuyển thuộc tính có danh sách lựa chọn "
                            + "sang kiểu dữ liệu khác khi option còn hoạt động"
            );
        }

        attribute.setName(request.name().trim());
        attribute.setCode(code);
        attribute.setDataType(request.dataType());
        attribute.setUnit(normalizeUnit(request.unit()));
        attribute.setRequired(Boolean.TRUE.equals(request.required()));

        return attributeMapper.toResponse(
                attributeRepository.save(attribute)
        );
    }

    @Override
    @Transactional
    public void deactivate(UUID attributeId) {
        Attribute attribute = getActiveAttribute(attributeId);

        attribute.setActive(false);
        attributeRepository.save(attribute);
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

    private boolean hasStoredValues(UUID attributeId) {
        return productAttributeValueRepository.existsByAttributeId(attributeId)
                || variantAttributeValueRepository.existsByAttributeId(attributeId);
    }

    private String normalizeCode(String code) {
        if (code == null || code.isBlank()) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_ERROR,
                    "Mã thuộc tính không được để trống"
            );
        }

        return code.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeUnit(String unit) {
        return unit == null || unit.isBlank() ? null : unit.trim();
    }
}
