package com.dtthuan3.ecommerce.productservice.service.impl;

import com.dtthuan3.ecommerce.productservice.dto.request.ProductVariantRequest;
import com.dtthuan3.ecommerce.productservice.dto.request.VariantAttributeValueRequest;
import com.dtthuan3.ecommerce.productservice.dto.response.ProductVariantResponse;
import com.dtthuan3.ecommerce.productservice.dto.response.VariantAttributeValueResponse;
import com.dtthuan3.ecommerce.productservice.domain.ProductVariant;
import com.dtthuan3.ecommerce.productservice.domain.VariantAttributeValue;
import com.dtthuan3.ecommerce.productservice.exception.DuplicateResourceException;
import com.dtthuan3.ecommerce.productservice.exception.ResourceNotFoundException;
import com.dtthuan3.ecommerce.productservice.mapper.ProductVariantMapper;
import com.dtthuan3.ecommerce.productservice.mapper.VariantAttributeValueMapper;
import com.dtthuan3.ecommerce.productservice.repository.ProductVariantRepository;
import com.dtthuan3.ecommerce.productservice.repository.VariantAttributeValueRepository;
import com.dtthuan3.ecommerce.productservice.service.ProductVariantService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductVariantServiceImpl
        implements ProductVariantService {

    private final ProductVariantRepository productVariantRepository;
    private final VariantAttributeValueRepository
            variantAttributeValueRepository;

    private final ProductVariantMapper productVariantMapper;
    private final VariantAttributeValueMapper
            variantAttributeValueMapper;

    @Override
    @Transactional
    public ProductVariantResponse create(
            ProductVariantRequest request
    ) {

        if (productVariantRepository.existsByVariantCode(
                request.getVariantCode()
        )) {
            throw new DuplicateResourceException(
                    "Variant code đã tồn tại: "
                            + request.getVariantCode()
            );
        }

        ProductVariant variant =
                productVariantMapper.toEntity(request);

        ProductVariant saved =
                productVariantRepository.save(variant);

        return productVariantMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductVariantResponse getById(Long id) {

        ProductVariant variant =
                productVariantRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Không tìm thấy product variant với id: "
                                                + id
                                )
                        );

        return productVariantMapper.toResponse(variant);
    }

    @Override
    @Transactional
    public ProductVariantResponse update(
            Long id,
            ProductVariantRequest request
    ) {

        ProductVariant variant =
                productVariantRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Không tìm thấy product variant với id: "
                                                + id
                                )
                        );

        if (productVariantRepository
                .existsByVariantCodeAndIdNot(
                        request.getVariantCode(),
                        id
                )) {

            throw new DuplicateResourceException(
                    "Variant code đã tồn tại: "
                            + request.getVariantCode()
            );
        }

        productVariantMapper.updateEntity(request, variant);

        ProductVariant updated =
                productVariantRepository.save(variant);

        return productVariantMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public void delete(Long id) {

        ProductVariant variant =
                productVariantRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Không tìm thấy product variant với id: "
                                                + id
                                )
                        );

        productVariantRepository.delete(variant);
    }

    // =========================
    // Variant Attribute Value
    // =========================

    @Override
    @Transactional
    public VariantAttributeValueResponse createAttributeValue(
            VariantAttributeValueRequest request
    ) {

        if (!productVariantRepository.existsById(
                request.getVariantId()
        )) {
            throw new ResourceNotFoundException(
                    "Không tìm thấy variant với id: "
                            + request.getVariantId()
            );
        }

        if (variantAttributeValueRepository
                .existsByVariantIdAndAttributeId(
                        request.getVariantId(),
                        request.getAttributeId()
                )) {

            throw new DuplicateResourceException(
                    "Attribute đã tồn tại trong variant"
            );
        }

        VariantAttributeValue value =
                variantAttributeValueMapper.toEntity(request);

        VariantAttributeValue saved =
                variantAttributeValueRepository.save(value);

        return variantAttributeValueMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public VariantAttributeValueResponse getAttributeValueById(
            Long id
    ) {

        VariantAttributeValue value =
                variantAttributeValueRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Không tìm thấy variant attribute value với id: "
                                                + id
                                )
                        );

        return variantAttributeValueMapper.toResponse(value);
    }

    @Override
    @Transactional
    public VariantAttributeValueResponse updateAttributeValue(
            Long id,
            VariantAttributeValueRequest request
    ) {

        VariantAttributeValue value =
                variantAttributeValueRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Không tìm thấy variant attribute value với id: "
                                                + id
                                )
                        );

        if (!productVariantRepository.existsById(
                request.getVariantId()
        )) {
            throw new ResourceNotFoundException(
                    "Không tìm thấy variant với id: "
                            + request.getVariantId()
            );
        }

        if (variantAttributeValueRepository
                .existsByVariantIdAndAttributeIdAndIdNot(
                        request.getVariantId(),
                        request.getAttributeId(),
                        id
                )) {

            throw new DuplicateResourceException(
                    "Attribute đã tồn tại trong variant"
            );
        }

        variantAttributeValueMapper.updateEntity(request, value);

        VariantAttributeValue updated =
                variantAttributeValueRepository.save(value);

        return variantAttributeValueMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public void deleteAttributeValue(Long id) {

        VariantAttributeValue value =
                variantAttributeValueRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Không tìm thấy variant attribute value với id: "
                                                + id
                                )
                        );

        variantAttributeValueRepository.delete(value);
    }
}