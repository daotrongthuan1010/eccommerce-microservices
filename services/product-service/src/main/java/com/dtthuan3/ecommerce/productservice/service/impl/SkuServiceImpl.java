package com.dtthuan3.ecommerce.productservice.service.impl;

import com.dtthuan3.ecommerce.productservice.constant.SkuSellingStatus;
import com.dtthuan3.ecommerce.productservice.domain.ProductVariant;
import com.dtthuan3.ecommerce.productservice.domain.Sku;
import com.dtthuan3.ecommerce.productservice.dto.request.CreateSkuRequest;
import com.dtthuan3.ecommerce.productservice.repository.ProductVariantRepository;
import com.dtthuan3.ecommerce.productservice.repository.SkuRepository;
import com.dtthuan3.ecommerce.productservice.service.SkuService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SkuServiceImpl implements SkuService {

    private final ProductVariantRepository productVariantRepository;
    private final SkuRepository skuRepository;

    @Override
    @Transactional
    public Sku create(UUID variantId, CreateSkuRequest request) {
        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Không tìm thấy biến thể"));

        if (!Boolean.TRUE.equals(variant.getActive())) {
            throw new IllegalArgumentException(
                    "Không thể tạo SKU cho biến thể đã ngừng hoạt động"
            );
        }

        String skuCode = request.skuCode().trim();
        String barcode = request.barcode().trim();

        if (skuRepository.existsBySkuCodeIgnoreCase(skuCode)) {
            throw new IllegalArgumentException("Mã SKU đã tồn tại");
        }

        if (skuRepository.existsByBarcode(barcode)) {
            throw new IllegalArgumentException("Barcode đã tồn tại");
        }

        Sku sku = Sku.builder()
                .id(UUID.randomUUID())
                .variantId(variantId)
                .skuCode(skuCode)
                .barcode(barcode)
                .sellingStatus(SkuSellingStatus.SELLABLE)
                .build();

        return skuRepository.save(sku);
    }

    @Override
    @Transactional(readOnly = true)
    public Sku getById(UUID skuId) {
        return skuRepository.findById(skuId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Không tìm thấy SKU"));
    }
}