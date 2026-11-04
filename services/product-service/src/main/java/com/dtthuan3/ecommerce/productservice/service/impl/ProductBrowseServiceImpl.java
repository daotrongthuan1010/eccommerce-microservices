package com.dtthuan3.ecommerce.productservice.service.impl;

import com.dtthuan3.ecommerce.common.response.PageResponse;
import com.dtthuan3.ecommerce.productservice.domain.PriceSchedule;
import com.dtthuan3.ecommerce.productservice.domain.Product;
import com.dtthuan3.ecommerce.productservice.domain.ProductMedia;
import com.dtthuan3.ecommerce.productservice.domain.ProductRevision;
import com.dtthuan3.ecommerce.productservice.domain.ProductVariant;
import com.dtthuan3.ecommerce.productservice.domain.Sku;
import com.dtthuan3.ecommerce.productservice.constant.SkuSellingStatus;
import com.dtthuan3.ecommerce.productservice.dto.response.ProductCardResponse;
import com.dtthuan3.ecommerce.productservice.repository.PriceScheduleRepository;
import com.dtthuan3.ecommerce.productservice.repository.ProductMediaRepository;
import com.dtthuan3.ecommerce.productservice.repository.ProductRepository;
import com.dtthuan3.ecommerce.productservice.repository.ProductRevisionRepository;
import com.dtthuan3.ecommerce.productservice.repository.ProductVariantRepository;
import com.dtthuan3.ecommerce.productservice.repository.SkuRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Marketplace browse (2026-11-05 fe-marketplace D4).
 *
 * Cach lam: 2 buoc de tranh N+1 va khong join cheo bang trong page query:
 * 1. Page<Product> theo filter (ACTIVE + publishedRevisionId, category, brand).
 * 2. Batch enrich: revisions (IN publishedRevisionId) → variants (IN productId)
 *    → skus SELLABLE (IN variantId) → price-schedules ACTIVE (IN skuId)
 *    → media primary (IN productId). Map trong memory roi build card.
 *
 * Keyword + loc gia + sort theo gia ap dung SAU enrich (data hoc tap nho,
 * page size <= 100) — khong the lam trong SQL vi gia nam bang price_schedules
 * theo sku, ten nam bang product_revisions.
 */
@Service
@RequiredArgsConstructor
public class ProductBrowseServiceImpl implements com.dtthuan3.ecommerce.productservice.service.ProductBrowseService {

    private static final int MAX_SIZE = 100;

    private final ProductRepository productRepository;
    private final ProductRevisionRepository productRevisionRepository;
    private final ProductVariantRepository productVariantRepository;
    private final SkuRepository skuRepository;
    private final PriceScheduleRepository priceScheduleRepository;
    private final ProductMediaRepository productMediaRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<ProductCardResponse> browse(
            int page,
            int size,
            UUID categoryId,
            UUID brandId,
            Long minPrice,
            Long maxPrice,
            String keyword,
            String sort
    ) {
        int safeSize = Math.min(Math.max(size, 1), MAX_SIZE);
        int safePage = Math.max(page, 0);

        // Buoc 1: page products da xuat ban theo filter SQL duoc (category/brand).
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "updatedAt"));
        Page<Product> products = productRepository.findPublishedForBrowse(categoryId, brandId, pageable);

        List<Product> content = products.getContent();
        if (content.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, products.getTotalElements());
        }

        List<UUID> productIds = content.stream().map(Product::getId).toList();
        Set<UUID> revisionIds = content.stream()
                .map(Product::getPublishedRevisionId)
                .collect(Collectors.toSet());

        // Buoc 2: batch enrich.
        Map<UUID, ProductRevision> revisions = productRevisionRepository
                .findAllByIdIn(revisionIds).stream()
                .collect(Collectors.toMap(ProductRevision::getId, Function.identity()));

        List<ProductVariant> variants = productVariantRepository
                .findAllByProductIdInAndActiveTrue(productIds);
        Map<UUID, List<ProductVariant>> variantsByProduct = variants.stream()
                .collect(Collectors.groupingBy(ProductVariant::getProductId));

        List<UUID> variantIds = variants.stream().map(ProductVariant::getId).toList();
        List<Sku> skus = variantIds.isEmpty()
                ? List.of()
                : skuRepository.findAllByVariantIdInAndSellingStatus(variantIds, SkuSellingStatus.SELLABLE);
        Map<UUID, List<Sku>> skusByVariant = skus.stream()
                .collect(Collectors.groupingBy(Sku::getVariantId));

        List<UUID> skuIds = skus.stream().map(Sku::getId).toList();
        LocalDateTime now = LocalDateTime.now();
        List<PriceSchedule> schedules = skuIds.isEmpty()
                ? List.of()
                : priceScheduleRepository.findActiveForSkus(skuIds, now);
        Map<UUID, List<PriceSchedule>> schedulesBySku = schedules.stream()
                .collect(Collectors.groupingBy(PriceSchedule::getSkuId));

        Map<UUID, ProductMedia> primaryMedia = productMediaRepository
                .findByProductIdInAndPrimaryTrue(productIds).stream()
                .collect(Collectors.toMap(
                        ProductMedia::getProductId,
                        Function.identity(),
                        // Nhieu anh primary do du lieu cu: lay display order nho nhat.
                        (a, b) -> a.getDisplayOrder() <= b.getDisplayOrder() ? a : b
                ));

        // Build card + tinh gia tu (min gia ACTIVE cua SKU SELLABLE).
        List<ProductCardResponse> cards = content.stream()
                .map(p -> buildCard(p, revisions, variantsByProduct, skusByVariant, schedulesBySku, primaryMedia))
                .toList();

        // Loc keyword/gia + sort gia o lop memory (giai thich o class javadoc).
        List<ProductCardResponse> filtered = filterAndSort(cards, minPrice, maxPrice, keyword, sort);

        // Filter lam giam so phan tu trong page nhung totalElements van theo
        // page SQL — dung total goc (data hoc tap, page nho; note trong ADR).
        return new PageImpl<>(filtered, pageable, products.getTotalElements());
    }

    private ProductCardResponse buildCard(
            Product product,
            Map<UUID, ProductRevision> revisions,
            Map<UUID, List<ProductVariant>> variantsByProduct,
            Map<UUID, List<Sku>> skusByVariant,
            Map<UUID, List<PriceSchedule>> schedulesBySku,
            Map<UUID, ProductMedia> primaryMedia
    ) {
        ProductRevision revision = revisions.get(product.getPublishedRevisionId());

        List<Sku> sellableSkus = variantsByProduct
                .getOrDefault(product.getId(), List.of()).stream()
                .flatMap(v -> skusByVariant.getOrDefault(v.getId(), List.of()).stream())
                .toList();

        long priceFrom = sellableSkus.stream()
                .flatMap(sku -> schedulesBySku.getOrDefault(sku.getId(), List.of()).stream())
                .map(PriceSchedule::getPrice)
                .filter(java.util.Objects::nonNull)
                .mapToLong(p -> p.setScale(0, java.math.RoundingMode.HALF_UP).longValueExact())
                .min()
                .orElse(0L);

        ProductMedia media = primaryMedia.get(product.getId());

        return ProductCardResponse.builder()
                .id(product.getId())
                .productCode(product.getProductCode())
                .name(revision != null ? revision.getName() : product.getProductCode())
                .shortDescription(revision != null ? revision.getShortDescription() : null)
                .categoryId(product.getCategoryId())
                .brandId(product.getBrandId())
                .priceFrom(priceFrom)
                .currency("VND")
                .primaryImageKey(media != null ? media.getFileKey() : null)
                .sellableSkuCount(sellableSkus.size())
                .build();
    }

    private List<ProductCardResponse> filterAndSort(
            List<ProductCardResponse> cards,
            Long minPrice,
            Long maxPrice,
            String keyword,
            String sort
    ) {
        java.util.stream.Stream<ProductCardResponse> stream = cards.stream();

        if (minPrice != null) {
            stream = stream.filter(c -> c.getPriceFrom() >= minPrice);
        }
        if (maxPrice != null) {
            stream = stream.filter(c -> c.getPriceFrom() > 0 && c.getPriceFrom() <= maxPrice);
        }
        String kw = (keyword == null || keyword.isBlank()) ? null : keyword.trim().toLowerCase();
        if (kw != null) {
            stream = stream.filter(c -> c.getName() != null && c.getName().toLowerCase().contains(kw));
        }

        List<ProductCardResponse> out = stream.collect(Collectors.toList());
        if ("price_asc".equals(sort)) {
            out.sort(Comparator.comparingLong(ProductCardResponse::getPriceFrom));
        } else if ("price_desc".equals(sort)) {
            out.sort(Comparator.comparingLong(ProductCardResponse::getPriceFrom).reversed());
        }
        // "newest" hoac khac: giu thu tu updatedAt DESC tu SQL.
        return out;
    }
}
