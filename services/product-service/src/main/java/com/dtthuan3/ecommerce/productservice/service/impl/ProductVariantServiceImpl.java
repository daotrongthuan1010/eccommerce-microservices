package com.dtthuan3.ecommerce.productservice.service.impl;

import com.dtthuan3.ecommerce.productservice.domain.Product;
import com.dtthuan3.ecommerce.productservice.domain.ProductVariant;
import com.dtthuan3.ecommerce.productservice.dto.request.CreateProductVariantRequest;
import com.dtthuan3.ecommerce.productservice.repository.ProductRepository;
import com.dtthuan3.ecommerce.productservice.repository.ProductVariantRepository;
import com.dtthuan3.ecommerce.productservice.service.ProductVariantService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductVariantServiceImpl implements ProductVariantService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;

    @Override
    @Transactional
    public ProductVariant create(
            UUID productId,
            CreateProductVariantRequest request
    ) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Không tìm thấy sản phẩm"));

        if (productVariantRepository.existsByProductIdAndVariantCode(
                productId,
                request.variantCode().trim()
        )) {
            throw new IllegalArgumentException(
                    "Mã biến thể đã tồn tại trong sản phẩm"
            );
        }

        ProductVariant variant = ProductVariant.builder()
                .id(UUID.randomUUID())
                .productId(productId)
                .variantCode(request.variantCode().trim())
                .name(request.name().trim())
                .description(request.description())
                .active(true)
                .build();

        return productVariantRepository.save(variant);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductVariant getById(UUID variantId) {
        return productVariantRepository.findById(variantId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Không tìm thấy biến thể"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductVariant> getByProductId(UUID productId) {
        if (!productRepository.existsById(productId)) {
            throw new IllegalArgumentException("Không tìm thấy sản phẩm");
        }

        return productVariantRepository
                .findAllByProductIdOrderByCreatedAtDesc(productId);
    }
}