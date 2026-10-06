package com.dtthuan3.ecommerce.productservice.service.impl;

import com.dtthuan3.ecommerce.productservice.dto.request.ProductMediaRequest;
import com.dtthuan3.ecommerce.productservice.dto.request.SkuMediaRequest;
import com.dtthuan3.ecommerce.productservice.dto.response.ProductMediaResponse;
import com.dtthuan3.ecommerce.productservice.dto.response.SkuMediaResponse;
import com.dtthuan3.ecommerce.productservice.domain.ProductMedia;
import com.dtthuan3.ecommerce.productservice.domain.SkuMedia;
import com.dtthuan3.ecommerce.productservice.exception.ResourceNotFoundException;
import com.dtthuan3.ecommerce.productservice.mapper.ProductMediaMapper;
import com.dtthuan3.ecommerce.productservice.mapper.SkuMediaMapper;
import com.dtthuan3.ecommerce.productservice.repository.ProductMediaRepository;
import com.dtthuan3.ecommerce.productservice.repository.SkuMediaRepository;
import com.dtthuan3.ecommerce.productservice.service.ProductMediaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductMediaServiceImpl
        implements ProductMediaService {

    private final ProductMediaRepository productMediaRepository;
    private final SkuMediaRepository skuMediaRepository;

    private final ProductMediaMapper productMediaMapper;
    private final SkuMediaMapper skuMediaMapper;

    // =========================
    // Product Media
    // =========================

    @Override
    @Transactional
    public ProductMediaResponse createProductMedia(
            ProductMediaRequest request
    ) {

        ProductMedia media =
                productMediaMapper.toEntity(request);

        ProductMedia saved =
                productMediaRepository.save(media);

        return productMediaMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductMediaResponse getProductMediaById(
            Long id
    ) {

        ProductMedia media =
                productMediaRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Không tìm thấy product media với id: "
                                                + id
                                )
                        );

        return productMediaMapper.toResponse(media);
    }

    @Override
    @Transactional
    public ProductMediaResponse updateProductMedia(
            Long id,
            ProductMediaRequest request
    ) {

        ProductMedia media =
                productMediaRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Không tìm thấy product media với id: "
                                                + id
                                )
                        );

        productMediaMapper.updateEntity(request, media);

        ProductMedia updated =
                productMediaRepository.save(media);

        return productMediaMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public void deleteProductMedia(Long id) {

        ProductMedia media =
                productMediaRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Không tìm thấy product media với id: "
                                                + id
                                )
                        );

        productMediaRepository.delete(media);
    }

    // =========================
    // SKU Media
    // =========================

    @Override
    @Transactional
    public SkuMediaResponse createSkuMedia(
            SkuMediaRequest request
    ) {

        SkuMedia media =
                skuMediaMapper.toEntity(request);

        SkuMedia saved =
                skuMediaRepository.save(media);

        return skuMediaMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public SkuMediaResponse getSkuMediaById(Long id) {

        SkuMedia media =
                skuMediaRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Không tìm thấy SKU media với id: "
                                                + id
                                )
                        );

        return skuMediaMapper.toResponse(media);
    }

    @Override
    @Transactional
    public SkuMediaResponse updateSkuMedia(
            Long id,
            SkuMediaRequest request
    ) {

        SkuMedia media =
                skuMediaRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Không tìm thấy SKU media với id: "
                                                + id
                                )
                        );

        skuMediaMapper.updateEntity(request, media);

        SkuMedia updated =
                skuMediaRepository.save(media);

        return skuMediaMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public void deleteSkuMedia(Long id) {

        SkuMedia media =
                skuMediaRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Không tìm thấy SKU media với id: "
                                                + id
                                )
                        );

        skuMediaRepository.delete(media);
    }
}