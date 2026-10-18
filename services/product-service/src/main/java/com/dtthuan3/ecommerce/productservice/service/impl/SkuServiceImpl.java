package com.dtthuan3.ecommerce.productservice.service.impl;

import com.dtthuan3.ecommerce.productservice.dto.request.SkuRequest;
import com.dtthuan3.ecommerce.productservice.dto.response.SkuResponse;
import com.dtthuan3.ecommerce.productservice.domain.Sku;
import com.dtthuan3.ecommerce.common.exception.DuplicateResourceException;
import com.dtthuan3.ecommerce.common.exception.ResourceNotFoundException;
import com.dtthuan3.ecommerce.productservice.mapper.SkuMapper;
import com.dtthuan3.ecommerce.productservice.repository.SkuRepository;
import com.dtthuan3.ecommerce.productservice.service.SkuService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SkuServiceImpl implements SkuService {

    private final SkuRepository skuRepository;
    private final SkuMapper skuMapper;

    @Override
    @Transactional
    public SkuResponse create(SkuRequest request) {

        if (skuRepository.existsBySkuCode(
                request.getSkuCode()
        )) {
            throw new DuplicateResourceException(
                    "SKU code đã tồn tại: "
                            + request.getSkuCode()
            );
        }

        if (skuRepository.existsByBarcode(
                request.getBarcode()
        )) {
            throw new DuplicateResourceException(
                    "Barcode đã tồn tại: "
                            + request.getBarcode()
            );
        }

        Sku sku = skuMapper.toEntity(request);

        Sku saved = skuRepository.save(sku);

        return skuMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public SkuResponse getById(Long id) {

        Sku sku = skuRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Không tìm thấy SKU với id: " + id
                        )
                );

        return skuMapper.toResponse(sku);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> findActiveSkuCodes(Collection<String> skuCodes) {
        if (skuCodes == null || skuCodes.isEmpty()) {
            return List.of();
        }
        Set<String> requested = skuCodes.stream()
                .filter(code -> code != null && !code.isBlank())
                .collect(Collectors.toSet());
        if (requested.isEmpty()) {
            return List.of();
        }
        return skuRepository.findActiveSkuCodesBySkuCodeIn(List.copyOf(requested));
    }

    @Override
    @Transactional
    public SkuResponse update(
            Long id,
            SkuRequest request
    ) {

        Sku sku = skuRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Không tìm thấy SKU với id: " + id
                        )
                );

        if (skuRepository.existsBySkuCodeAndIdNot(
                request.getSkuCode(),
                id
        )) {
            throw new DuplicateResourceException(
                    "SKU code đã tồn tại: "
                            + request.getSkuCode()
            );
        }

        if (skuRepository.existsByBarcodeAndIdNot(
                request.getBarcode(),
                id
        )) {
            throw new DuplicateResourceException(
                    "Barcode đã tồn tại: "
                            + request.getBarcode()
            );
        }

        skuMapper.updateEntity(request, sku);

        Sku updated = skuRepository.save(sku);

        return skuMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public void delete(Long id) {

        Sku sku = skuRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Không tìm thấy SKU với id: " + id
                        )
                );

        skuRepository.delete(sku);
    }
}
