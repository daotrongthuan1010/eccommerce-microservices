package com.dtthuan3.ecommerce.catalogservice.service.impl;

import com.dtthuan3.ecommerce.catalogservice.contstant.CacheTtl;
import com.dtthuan3.ecommerce.catalogservice.dto.request.CategoryCreateRequest;
import com.dtthuan3.ecommerce.catalogservice.dto.request.CategoryUpdateRequest;
import com.dtthuan3.ecommerce.catalogservice.dto.response.CategoryResponse;
import com.dtthuan3.ecommerce.catalogservice.dto.response.CategoryTreeResponse;
import com.dtthuan3.ecommerce.catalogservice.entity.Category;
import com.dtthuan3.ecommerce.catalogservice.mapper.CategoryMapper;
import com.dtthuan3.ecommerce.catalogservice.repository.CategoryRepository;
import com.dtthuan3.ecommerce.catalogservice.service.CacheKeys;
import com.dtthuan3.ecommerce.catalogservice.service.CategoryService;
import com.dtthuan3.ecommerce.catalogservice.service.RedisCacheService;
import com.dtthuan3.ecommerce.common.exception.BusinessException;
import com.dtthuan3.ecommerce.common.exception.ErrorCode;
import com.dtthuan3.ecommerce.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private static final int MAX_DEPTH = 5;

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;
    private final RedisCacheService redisCacheService;

    @Override
    @Transactional
    public CategoryResponse create(
            CategoryCreateRequest request
    ) {

        validateParent(
                request.getParentId(),
                null
        );

        validateSlug(
                request.getSlug(),
                request.getParentId(),
                null
        );

        validateDepth(
                request.getParentId()
        );

        Category category =
                categoryMapper.toEntity(request);

        category.setRevision(0L);

        Category saved =
                categoryRepository.save(category);

        evictCategoryCaches(
                saved.getId(),
                saved.getParentId()
        );

        return categoryMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getById(String id) {

        String cacheKey =
                CacheKeys.CATEGORY + id;

        Optional<CategoryResponse> cached =
                redisCacheService.get(
                        cacheKey,
                        CategoryResponse.class
                );

        if (cached.isPresent()) {
            return cached.get();
        }

        Category category =
                findCategory(id);

        CategoryResponse response =
                categoryMapper.toResponse(category);

        redisCacheService.set(
                cacheKey,
                response,
                CacheTtl.MEDIUM
        );

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getChildren(
            String parentId
    ) {

        String cacheKey =
                CacheKeys.CATEGORY_CHILDREN + parentId;

        List<CategoryResponse> cached =
                redisCacheService.getList(
                        cacheKey,
                        CategoryResponse.class
                );

        if (!cached.isEmpty()
                || redisCacheService.hasKey(cacheKey)) {

            return cached;
        }

        if (!categoryRepository.existsById(parentId)) {
            throw new ResourceNotFoundException(
                    "Category",
                    parentId
            );
        }

        List<CategoryResponse> result =
                categoryRepository
                        .findByParentIdOrderByDisplayOrderAsc(parentId)
                        .stream()
                        .map(categoryMapper::toResponse)
                        .toList();

        redisCacheService.set(
                cacheKey,
                result,
                CacheTtl.MEDIUM
        );

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getRootCategories() {

        String cacheKey =
                CacheKeys.CATEGORY_ROOTS;

        List<CategoryResponse> cached =
                redisCacheService.getList(
                        cacheKey,
                        CategoryResponse.class
                );

        if (!cached.isEmpty()
                || redisCacheService.hasKey(cacheKey)) {

            return cached;
        }

        List<CategoryResponse> result =
                categoryRepository
                        .findByParentIdIsNullOrderByDisplayOrderAsc()
                        .stream()
                        .map(categoryMapper::toResponse)
                        .toList();

        redisCacheService.set(cacheKey, result, CacheTtl.MEDIUM);

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryTreeResponse> getTree() {

        String cacheKey = CacheKeys.CATEGORY_TREE;

        List<CategoryTreeResponse> cached =
                redisCacheService.getList(
                        cacheKey,
                        CategoryTreeResponse.class
                );

        if (!cached.isEmpty() || redisCacheService.hasKey(cacheKey)) {

            return cached;
        }

        List<Category> categories = categoryRepository.findAllForTree();

        List<CategoryTreeResponse> result = buildTree(categories);

        redisCacheService.set(cacheKey, result, CacheTtl.MEDIUM);

        return result;
    }

    @Override
    @Transactional
    public CategoryResponse update(
            String id,
            CategoryUpdateRequest request
    ) {

        Category category = findCategory(id);

        validateParent(request.getParentId(), id);

        validateSlug(
                request.getSlug(),
                request.getParentId(),
                id
        );

        validateDepth(request.getParentId());

        validateNoCycle(id, request.getParentId());

        String oldParentId = category.getParentId();

        categoryMapper.updateEntity(category, request);

        category.setRevision(category.getRevision() + 1);

        Category updated = categoryRepository.save(category);

        evictCategoryCaches(updated.getId(), oldParentId);

        evictCategoryCaches(updated.getId(), updated.getParentId());

        return categoryMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public void delete(String id) {

        Category category =
                findCategory(id);

        /*
         * CAT-UC02:
         * Không xóa vật lý node đang có category con.
         */
        if (categoryRepository.existsByParentId(id)) {

            throw new BusinessException(
                    ErrorCode.CONFLICT,
                    "Không thể xóa danh mục đang có danh mục con"
            );
        }

        /*
         * Khi triển khai Product-Category mapping:
         *
         * if (productCategoryRepository.existsByCategoryId(id)) {
         *     throw new BusinessException(
         *         ErrorCode.CONFLICT,
         *         "Danh mục đang được sản phẩm sử dụng"
         *     );
         * }
         */

        String parentId =
                category.getParentId();

        categoryRepository.delete(category);

        evictCategoryCaches(
                id,
                parentId
        );
    }

    private Category findCategory(String id) {

        return categoryRepository
                .findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Category",
                                id
                        )
                );
    }

    private void validateParent(String parentId, String currentId) {

        if (parentId == null || parentId.isBlank()) {

            return;
        }

        if (parentId.equals(currentId)) {
            throw new BusinessException(
                    ErrorCode.CONFLICT,
                    "Danh mục không thể là cha của chính nó"
            );
        }

        if (!categoryRepository.existsById(parentId)) {

            throw new ResourceNotFoundException(
                    "Parent category",
                    parentId
            );
        }
    }

    private void validateSlug(
            String slug,
            String parentId,
            String currentId
    ) {

        boolean duplicated;

        if (currentId == null) {

            duplicated =
                    categoryRepository
                            .existsBySlugAndParentId(
                                    slug,
                                    parentId
                            );

        } else {

            duplicated =
                    categoryRepository
                            .existsBySlugAndParentIdAndIdNot(
                                    slug,
                                    parentId,
                                    currentId
                            );
        }

        if (duplicated) {

            throw new BusinessException(
                    ErrorCode.CONFLICT,
                    "Slug đã tồn tại trong cùng danh mục cha: "
                            + slug
            );
        }
    }

    private void validateDepth(String parentId) {

        int depth = 1;

        String currentParentId = parentId;

        Set<String> visited = new HashSet<>();

        while (currentParentId != null) {

            if (!visited.add(currentParentId)) {

                throw new BusinessException(
                        ErrorCode.CONFLICT,
                        "Cây danh mục đang chứa chu trình"
                );
            }

            depth++;

            if (depth > MAX_DEPTH) {

                throw new BusinessException(
                        ErrorCode.CONFLICT,
                        "Danh mục chỉ được tối đa "
                                + MAX_DEPTH
                                + " tầng"
                );
            }

            Category parent = findCategory(currentParentId);

            currentParentId = parent.getParentId();
        }
    }

    private void validateNoCycle(String categoryId, String newParentId) {

        if (newParentId == null || newParentId.isBlank()) {

            return;
        }

        Set<String> visited =
                new HashSet<>();

        String currentId =
                newParentId;

        while (currentId != null) {

            if (!visited.add(currentId)) {

                throw new BusinessException(
                        ErrorCode.CONFLICT,
                        "Cây danh mục đang chứa chu trình"
                );
            }

            if (currentId.equals(categoryId)) {

                throw new BusinessException(
                        ErrorCode.CONFLICT,
                        "Không thể di chuyển danh mục vào node con của chính nó"
                );
            }

            Category current = findCategory(currentId);

            currentId = current.getParentId();
        }
    }

    private List<CategoryTreeResponse> buildTree(
            List<Category> categories
    ) {

        Map<String, CategoryTreeResponse> nodeMap = new HashMap<>();

        for (Category category : categories) {

            CategoryTreeResponse node =
                    categoryMapper.toTreeResponse(category);

            nodeMap.put(category.getId(), node);
        }

        List<CategoryTreeResponse> roots = new ArrayList<>();

        for (Category category : categories) {

            CategoryTreeResponse node = nodeMap.get(category.getId());

            String parentId = category.getParentId();

            if (parentId == null) {

                roots.add(node);

            } else {

                CategoryTreeResponse parent =
                        nodeMap.get(parentId);

                if (parent != null) {

                    parent.getChildren().add(node);
                }
            }
        }

        return roots;
    }

    private void evictCategoryCaches(
            String categoryId,
            String parentId
    ) {

        redisCacheService.delete(
                CacheKeys.CATEGORY + categoryId
        );

        redisCacheService.delete(
                CacheKeys.CATEGORY_TREE
        );

        redisCacheService.delete(
                CacheKeys.CATEGORY_ROOTS
        );

        if (parentId != null) {

            redisCacheService.delete(
                    CacheKeys.CATEGORY_CHILDREN
                            + parentId
            );
        }
    }
}