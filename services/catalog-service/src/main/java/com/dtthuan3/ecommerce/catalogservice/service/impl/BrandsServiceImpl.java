package com.dtthuan3.ecommerce.catalogservice.service.impl;


import com.dtthuan3.ecommerce.catalogservice.contstant.BrandsStatus;
import com.dtthuan3.ecommerce.catalogservice.dto.request.BrandsRequest;
import com.dtthuan3.ecommerce.catalogservice.dto.response.BrandsRespone;
import com.dtthuan3.ecommerce.catalogservice.dto.response.PageResponse;
import com.dtthuan3.ecommerce.catalogservice.entity.Brands;
import com.dtthuan3.ecommerce.catalogservice.mapper.BrandsMapper;
import com.dtthuan3.ecommerce.catalogservice.mapper.PageMapper;
import com.dtthuan3.ecommerce.catalogservice.repository.BrandsRepository;
import com.dtthuan3.ecommerce.catalogservice.service.BrandsService;
import com.dtthuan3.ecommerce.catalogservice.service.CacheKeys;
import com.dtthuan3.ecommerce.catalogservice.service.MinioService;
import com.dtthuan3.ecommerce.catalogservice.service.RedisCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class BrandsServiceImpl implements BrandsService {

    private static final Duration CACHE_TTL =
            Duration.ofMinutes(30);

    private final BrandsRepository brandsRepository;

    private final BrandsMapper brandsMapper;

    private final MinioService minioService;

    private final RedisCacheService redisCacheService;

    @Override
    public BrandsRespone create(
            BrandsRequest request,
            MultipartFile image
    ) {

        validateSlugForCreate(request.getSlug());

        Brands brands = brandsMapper.toEntity(request);

        // Upload logo lên MinIO
        if (image != null && !image.isEmpty()) {

            String objectKey =
                    minioService.upload(
                            image,
                            "brands"
                    );

            brands.setLogoObjectKey(objectKey);
        }

        Brands savedBrands =
                brandsRepository.save(brands);

        return toResponse(savedBrands);
    }

    @Override
    @Transactional(readOnly = true)
    public BrandsRespone getByID(Long id) {

        String cacheKey = CacheKeys.BRAND + id;

        var cached = redisCacheService.get(
                        cacheKey,
                        BrandsRespone.class
                );

        if (cached.isPresent()) {
            return cached.get();
        }

        Brands brands = findBrandById(id);

        BrandsRespone response = toResponse(brands);

        redisCacheService.set(cacheKey, response, CACHE_TTL);

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<BrandsRespone> getAll() {

        return brandsRepository
                .findAllByStatusOrderByNameAsc(
                        com.dtthuan3.ecommerce.catalogservice.contstant.BrandsStatus.ACTIVE
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public BrandsRespone update(
            Long id,
            BrandsRequest request,
            MultipartFile image
    ) {

        Brands brands = findBrandById(id);

        validateSlugForUpdate(request.getSlug(), id);

        String oldLogoObjectKey = brands.getLogoObjectKey();

        brandsMapper.updateEntity(brands, request);

        // Nếu có logo mới
        if (image != null && !image.isEmpty()) {

            String newLogoObjectKey =
                    minioService.upload(image, "brands");

            brands.setLogoObjectKey(newLogoObjectKey);
        }

        Brands updatedBrands = brandsRepository.save(brands);

        // Xóa logo cũ
        if (
                image != null
                        && !image.isEmpty()
                        && oldLogoObjectKey != null
                        && !oldLogoObjectKey.isBlank()
        ) {

            minioService.delete(
                    oldLogoObjectKey
            );
        }

        // Xóa cache
        redisCacheService.delete(
                CacheKeys.BRAND + id
        );

        return toResponse(updatedBrands);
    }

    @Override
    public void delete(Long id) {

        Brands brands = findBrandById(id);

        String logoObjectKey = brands.getLogoObjectKey();

        brandsRepository.delete(brands);

        // Xóa cache
        redisCacheService.delete(CacheKeys.BRAND + id);

        // Xóa logo trên MinIO
        if (logoObjectKey != null
                        && !logoObjectKey.isBlank()) {

            minioService.delete(
                    logoObjectKey
            );
        }
    }

    @Override
    public PageResponse<BrandsRespone> search(String keyword, int page, int size, String sortBy, String direction) {
        return null;
    }

    private Brands findBrandById(Long id) {

        return brandsRepository
                .findById(id)
                .orElseThrow(
                        () -> new RuntimeException(
                                "Không tìm thấy thương hiệu với id: "
                                        + id
                        )
                );
    }


//    @Override
//    public PageResponse<BrandsRespone> search(
//            String keyword,
//            int page,
//            int size,
//            String sortBy,
//            String direction
//    ) {
//
//        // Không cho page âm
//        page = Math.max(page, 0);
//
//        // Không cho size <= 0 hoặc quá lớn
//        size = Math.min(Math.max(size, 1), 100);
//
//        // Các field được phép sort
//        if (!Set.of("name", "createdAt", "updatedAt")
//                .contains(sortBy)) {
//
//            sortBy = "name";
//        }
//
//        Sort sort = direction.equalsIgnoreCase("desc")
//                ? Sort.by(sortBy).descending()
//                : Sort.by(sortBy).ascending();
//
//        Pageable pageable = PageRequest.of(
//                page,
//                size,
//                sort
//        );
//
//        String searchKeyword = keyword == null
//                ? ""
//                : keyword.trim();
//
//        Page<Brands> result = brandsRepository.search(
//                searchKeyword,
//                BrandsStatus.ACTIVE,
//                pageable
//        );
//
//        return PageMapper.toResponse(
//                result,
//                brandsMapper::toResponse
//        );
//    }


    private void validateSlugForCreate(String slug) {

        if (brandsRepository.existsBySlug(slug)) {

            throw new IllegalArgumentException(
                    "Slug thương hiệu đã tồn tại: "
                            + slug
            );
        }
    }

    private void validateSlugForUpdate(
            String slug,
            Long id
    ) {

        if (brandsRepository
                        .existsBySlugAndIdNot(
                                slug,
                                id
                        )
        ) {

            throw new IllegalArgumentException(
                    "Slug thương hiệu đã tồn tại: "
                            + slug
            );
        }
    }

    private BrandsRespone toResponse(Brands brands) {

        String logoUrl =
                getLogoUrl(brands);

        return brandsMapper.toResponse(
                brands,
                logoUrl
        );
    }

    private String getLogoUrl(Brands brands) {

        if (
                brands.getLogoObjectKey() == null
                        || brands.getLogoObjectKey().isBlank()
        ) {
            return null;
        }

        return minioService.getPresignedUrl(
                brands.getLogoObjectKey()
        );
    }
}