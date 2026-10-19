
package com.dtthuan3.ecommerce.productservice.service.impl;

import com.dtthuan3.ecommerce.common.exception.BusinessException;
import com.dtthuan3.ecommerce.common.exception.ErrorCode;
import com.dtthuan3.ecommerce.common.exception.ResourceNotFoundException;
import com.dtthuan3.ecommerce.productservice.constant.AttributeDataType;
import com.dtthuan3.ecommerce.productservice.domain.Attribute;
import com.dtthuan3.ecommerce.productservice.domain.AttributeOption;
import com.dtthuan3.ecommerce.productservice.dto.request.AttributeOptionCreateRequest;
import com.dtthuan3.ecommerce.productservice.dto.request.AttributeOptionUpdateRequest;
import com.dtthuan3.ecommerce.productservice.dto.response.AttributeOptionResponse;
import com.dtthuan3.ecommerce.productservice.mapper.AttributeOptionMapper;
import com.dtthuan3.ecommerce.productservice.repository.AttributeOptionRepository;
import com.dtthuan3.ecommerce.productservice.repository.AttributeRepository;
import com.dtthuan3.ecommerce.productservice.repository.ProductAttributeValueRepository;
import com.dtthuan3.ecommerce.productservice.repository.VariantAttributeValueRepository;
import com.dtthuan3.ecommerce.productservice.service.AttributeOptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AttributeOptionServiceImpl
        implements AttributeOptionService {

    private final AttributeRepository attributeRepository;
    private final AttributeOptionRepository attributeOptionRepository;
    private final ProductAttributeValueRepository productAttributeValueRepository;
    private final VariantAttributeValueRepository variantAttributeValueRepository;
    private final AttributeOptionMapper attributeOptionMapper;

    @Override
    @Transactional
    public AttributeOptionResponse create(
            UUID attributeId,
            AttributeOptionCreateRequest request
    ) {
        Attribute attribute = getActiveSelectAttribute(attributeId);
        String value = request.value().trim();

        if (attributeOptionRepository
                .existsByAttributeIdAndValueIgnoreCase(attributeId, value)) {
            throw new BusinessException(
                    ErrorCode.CONFLICT,
                    "Giá trị lựa chọn đã tồn tại trong thuộc tính"
            );
        }

        AttributeOption option = AttributeOption.builder()
                .attributeId(attribute.getId())
                .value(value)
                .displayOrder(request.displayOrder())
                .active(true)
                .build();

        return attributeOptionMapper.toResponse(
                attributeOptionRepository.save(option)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttributeOptionResponse> getActiveOptions(UUID attributeId) {
        getActiveSelectAttribute(attributeId);

        return attributeOptionRepository
                .findByAttributeIdAndActiveTrueOrderByDisplayOrderAscValueAsc(
                        attributeId
                )
                .stream()
                .map(attributeOptionMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public AttributeOptionResponse update(
            UUID attributeId,
            UUID optionId,
            AttributeOptionUpdateRequest request
    ) {
        getActiveSelectAttribute(attributeId);
        AttributeOption option = getOption(attributeId, optionId);

        String value = request.value().trim();

        if (attributeOptionRepository
                .existsByAttributeIdAndValueIgnoreCaseAndIdNot(
                        attributeId,
                        value,
                        optionId
                )) {
            throw new BusinessException(
                    ErrorCode.CONFLICT,
                    "Giá trị lựa chọn đã tồn tại trong thuộc tính"
            );
        }

        boolean valueChanged = !option.getValue().equalsIgnoreCase(value);
        boolean optionIsUsed =
                productAttributeValueRepository.existsByOptionId(optionId)
                        || variantAttributeValueRepository.existsByOptionId(optionId);

        if (valueChanged && optionIsUsed) {
            throw new BusinessException(
                    ErrorCode.CONFLICT,
                    "Option đã được sử dụng. Không thể đổi giá trị; "
                            + "hãy tạo option mới để bảo toàn dữ liệu lịch sử."
            );
        }

        option.setValue(value);
        option.setDisplayOrder(request.displayOrder());

        return attributeOptionMapper.toResponse(
                attributeOptionRepository.save(option)
        );
    }

    @Override
    @Transactional
    public void deactivate(UUID attributeId, UUID optionId) {
        getActiveSelectAttribute(attributeId);
        AttributeOption option = getOption(attributeId, optionId);

        option.setActive(false);
        attributeOptionRepository.save(option);
    }

    private Attribute getActiveSelectAttribute(UUID attributeId) {
        Attribute attribute = attributeRepository
                .findByIdAndActiveTrue(attributeId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Attribute",
                                attributeId
                        )
                );

        if (attribute.getDataType() != AttributeDataType.SELECT) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_ERROR,
                    "Chỉ thuộc tính kiểu SELECT mới có danh sách option"
            );
        }

        return attribute;
    }

    private AttributeOption getOption(UUID attributeId, UUID optionId) {
        return attributeOptionRepository
                .findByIdAndAttributeId(optionId, attributeId)
                .filter(option -> Boolean.TRUE.equals(option.getActive()))
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "AttributeOption",
                                optionId
                        )
                );
    }
}
