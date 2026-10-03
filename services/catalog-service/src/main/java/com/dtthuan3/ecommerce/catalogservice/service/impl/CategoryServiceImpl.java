package com.dtthuan3.ecommerce.catalogservice.service.impl;

import com.dtthuan3.ecommerce.catalogservice.contstant.CacheTtl;
import com.dtthuan3.ecommerce.catalogservice.dto.request.CategoryRequest;
import com.dtthuan3.ecommerce.catalogservice.dto.response.CategoryResponse;
import com.dtthuan3.ecommerce.catalogservice.entity.Category;
import com.dtthuan3.ecommerce.catalogservice.exception.ResourceNotFoundException;
import com.dtthuan3.ecommerce.catalogservice.mapper.CategoryMapper;
import com.dtthuan3.ecommerce.catalogservice.repository.CategoryRepository;
import com.dtthuan3.ecommerce.catalogservice.service.CacheKeys;
import com.dtthuan3.ecommerce.catalogservice.service.CategoryService;
import com.dtthuan3.ecommerce.catalogservice.service.MinioService;
import com.dtthuan3.ecommerce.catalogservice.service.RedisCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl
        implements CategoryService {

    private final CategoryRepository categoryRepository;

    private final CategoryMapper categoryMapper;

    private final MinioService storageService;

    private final RedisCacheService redisCacheService;

    @Override
    @Transactional
    public CategoryResponse create(
            CategoryRequest request,
            MultipartFile image
    ) {

        validateSlugForCreate(request.getSlug());

        validateParent(request.getParentId());

        Category category = categoryMapper.toEntity(request);

        if (image != null && !image.isEmpty()) {

            String objectKey =
                    storageService.upload(image, "categories");

            category.setImageUrl(objectKey);
        }

        Category saved = categoryRepository.save(category);

        /*
         * Category đã thay đổi.
         * Các cache liên quan phải được xóa.
         */
        clearCategoryCache();

        return toResponse(saved);
    }


    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getById(
            Long id
    ) {

        String cacheKey = CacheKeys.CATEGORY + id;

        Optional<CategoryResponse> cached = redisCacheService.get(
                        cacheKey,
                        CategoryResponse.class
                );

        if (cached.isPresent()) {
            return cached.get();
        }


        Category category = categoryRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Không tìm thấy category: "
                                                + id
                                )
                        );

        CategoryResponse response =
                toResponse(category);

        redisCacheService.set(cacheKey, response, CacheTtl.MEDIUM);

        return response;
    }


    @Override
    @Transactional
    public CategoryResponse update(
            Long id,
            CategoryRequest request,
            MultipartFile image) {

        Category category = categoryRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Không tìm thấy category: "
                                                + id
                                )
                        );

        validateSlugForUpdate(request.getSlug(), id);

        validateParentForUpdate(request.getParentId(), id);

        String oldImageObjectKey = category.getImageUrl();

        categoryMapper.updateEntity(category, request);

        if (image != null && !image.isEmpty()) {

            String newObjectKey = storageService.upload(
                            image,
                            "categories");

            category.setImageUrl(newObjectKey);

            if (oldImageObjectKey != null) {

                storageService.delete(oldImageObjectKey);
            }
        }

        Category updated = categoryRepository.save(category);

        clearCategoryCache();

        return toResponse(updated);
    }

    @Override
    @Transactional
    public void delete(Long id) {

        Category category = categoryRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Không tìm thấy category: "
                                                + id
                                )
                        );

        if (categoryRepository.existsByParentId(id)) {

            throw new IllegalArgumentException(
                    "Không thể xóa category vì vẫn còn category con"
            );
        }

        String imageObjectKey = category.getImageUrl();

        categoryRepository.delete(category);


        if (imageObjectKey != null) {
            storageService.delete(
                    imageObjectKey
            );
        }

        clearCategoryCache();
    }

    private void validateSlugForCreate(String slug) {

        if (categoryRepository.existsBySlug(slug)) {

            throw new IllegalArgumentException("Slug đã tồn tại: " + slug);
        }
    }

    private void validateSlugForUpdate(String slug, Long id) {

        if (categoryRepository.existsBySlugAndIdNot(slug, id)) {

            throw new IllegalArgumentException(
                    "Slug đã tồn tại: " + slug
            );
        }
    }

    private void validateParent(Long parentId) {

        if (parentId == null) {return;}

        if (!categoryRepository.existsById(parentId)) {

            throw new ResourceNotFoundException(
                    "Không tìm thấy category cha: "
                            + parentId
            );
        }
    }

    private void validateParentForUpdate(Long parentId, Long categoryId) {

        if (parentId == null) {return;}

        if (parentId.equals(categoryId)) {

            throw new IllegalArgumentException(
                    "Category không thể là cha của chính nó"
            );
        }

        validateParent(parentId);
    }

    private CategoryResponse toResponse(Category category) {

        return categoryMapper.toResponse(category, getImageUrl(category));
    }

    private String getImageUrl(Category category) {

        if (category.getImageUrl() == null) {return null;}

        return storageService.getPresignedUrl(category.getImageUrl());
    }

    private void clearCategoryCache() {

        redisCacheService.deleteByPattern(
                CacheKeys.CATEGORY_PATTERN
        );
    }
}