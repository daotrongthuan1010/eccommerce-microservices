package com.dtthuan3.ecommerce.catalogservice.service.impl;

import com.dtthuan3.ecommerce.catalogservice.client.ProductServiceClient;
import com.dtthuan3.ecommerce.catalogservice.contstant.CategoryStatus;
import com.dtthuan3.ecommerce.catalogservice.dto.request.ProductCategoryUpdateRequest;
import com.dtthuan3.ecommerce.catalogservice.dto.response.CategoryProductsResponse;
import com.dtthuan3.ecommerce.catalogservice.dto.response.ProductCategoriesResponse;
import com.dtthuan3.ecommerce.catalogservice.dto.response.ProductCategoryResponse;
import com.dtthuan3.ecommerce.catalogservice.dto.response.ProductExistsResponse;
import com.dtthuan3.ecommerce.catalogservice.entity.Category;
import com.dtthuan3.ecommerce.catalogservice.entity.ProductCategory;
import com.dtthuan3.ecommerce.catalogservice.mapper.ProductCategoryMapper;
import com.dtthuan3.ecommerce.catalogservice.repository.CategoryRepository;
import com.dtthuan3.ecommerce.catalogservice.repository.ProductCategoryRepository;
import com.dtthuan3.ecommerce.common.exception.BusinessException;
import com.dtthuan3.ecommerce.common.exception.ErrorCode;
import com.dtthuan3.ecommerce.common.exception.ResourceNotFoundException;
import com.dtthuan3.ecommerce.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductCategoryServiceImpl
        implements com.dtthuan3.ecommerce.catalogservice.service.ProductCategoryService {

    private final ProductCategoryRepository productCategoryRepository;

    private final CategoryRepository categoryRepository;

    private final ProductCategoryMapper productCategoryMapper;

    private final ProductServiceClient productServiceClient;

    @Override
    @Transactional(readOnly = true)
    public ProductCategoriesResponse getProductCategories(
            UUID productId
    ) {

        validateProductExists(productId);

        List<ProductCategory> mappings =
                productCategoryRepository.findByProductId(productId);

        List<ProductCategoryResponse> responses =
                mappings.stream()
                        .map(productCategoryMapper::toResponse)
                        .toList();

        String primaryCategoryId =
                mappings.stream()
                        .filter(ProductCategory::getPrimary)
                        .map(ProductCategory::getCategoryId)
                        .findFirst()
                        .orElse(null);

        return ProductCategoriesResponse.builder()
                .productId(productId)
                .primaryCategoryId(primaryCategoryId)
                .categories(responses)
                .build();
    }

    @Override
    @Transactional
    public ProductCategoriesResponse updateProductCategories(
            UUID productId,
            ProductCategoryUpdateRequest request
    ) {

        validateProductExists(productId);

        validateRequest(request);

        List<String> categoryIds =
                request.categoryIds();

        String primaryCategoryId =
                request.primaryCategoryId();

        validatePrimaryCategory(
                categoryIds,
                primaryCategoryId
        );

        validateCategories(categoryIds);

        List<ProductCategory> existingMappings =
                productCategoryRepository.findByProductId(productId);

        Map<String, ProductCategory> existingMap =
                existingMappings.stream()
                        .collect(Collectors.toMap(
                                ProductCategory::getCategoryId,
                                Function.identity()
                        ));

        /*
         * Xóa những mapping cũ không còn nằm trong request.
         */
        for (ProductCategory existing : existingMappings) {

            if (!categoryIds.contains(existing.getCategoryId())) {

                productCategoryRepository.delete(existing);
            }
        }

        /*
         * Tạo mới hoặc update mapping.
         */
        for (String categoryId : categoryIds) {

            ProductCategory mapping =
                    existingMap.get(categoryId);

            if (mapping == null) {

                mapping = new ProductCategory();

                mapping.setProductId(productId);
                mapping.setCategoryId(categoryId);
            }

            mapping.setPrimary(
                    categoryId.equals(primaryCategoryId)
            );

            productCategoryRepository.save(mapping);
        }

        return getProductCategories(productId);
    }

    @Override
    @Transactional
    public ProductCategoryResponse setPrimaryCategory(
            UUID productId,
            String categoryId
    ) {

        validateProductExists(productId);

        validateCategory(categoryId);

        ProductCategory mapping =
                productCategoryRepository
                        .findByProductIdAndCategoryId(
                                productId,
                                categoryId
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Product chưa được gán vào category này"
                                )
                        );

        Optional<ProductCategory> currentPrimary =
                productCategoryRepository
                        .findByProductIdAndPrimaryTrue(productId);

        /*
         * Bỏ primary cũ.
         */
        if (currentPrimary.isPresent()
                && !currentPrimary.get()
                .getCategoryId()
                .equals(categoryId)) {

            ProductCategory oldPrimary =
                    currentPrimary.get();

            oldPrimary.setPrimary(false);

            productCategoryRepository.save(oldPrimary);
        }

        /*
         * Set primary mới.
         */
        mapping.setPrimary(true);

        productCategoryRepository.save(mapping);

        return productCategoryMapper.toResponse(mapping);
    }

    @Override
    @Transactional
    public void removeProductFromCategory(
            UUID productId,
            String categoryId
    ) {

        validateProductExists(productId);

        ProductCategory mapping =
                productCategoryRepository
                        .findByProductIdAndCategoryId(
                                productId,
                                categoryId
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Không tìm thấy mapping Product - Category"
                                )
                        );

        /*
         * Không cho xóa primary nếu Product vẫn còn
         * các category khác mà chưa chọn primary mới.
         */
        if (Boolean.TRUE.equals(mapping.getPrimary())) {

            long count =
                    productCategoryRepository
                            .findByProductId(productId)
                            .size();

            if (count > 1) {

                throw new BusinessException(
                        ErrorCode.BAD_REQUEST,
                        "Không thể xóa primary category. " +
                                "Hãy chọn primary category khác trước"
                );
            }
        }

        productCategoryRepository.delete(mapping);
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryProductsResponse getCategoryProducts(
            String categoryId
    ) {

        validateCategory(categoryId);

        List<ProductCategory> mappings =
                productCategoryRepository
                        .findByCategoryId(categoryId);

        List<ProductCategoryResponse> responses =
                mappings.stream()
                        .map(productCategoryMapper::toResponse)
                        .toList();

        return CategoryProductsResponse.builder()
                .categoryId(categoryId)
                .products(responses)
                .build();
    }

    // =========================================================
    // VALIDATION
    // =========================================================

    private void validateProductExists(UUID productId) {

        try {

            ApiResponse<ProductExistsResponse> response =
                    productServiceClient.getById(productId);

            if (response == null
                    || response.getData() == null
                    || response.getData().getId() == null) {

                throw new ResourceNotFoundException(
                        "Product không tồn tại: " + productId
                );
            }

        } catch (ResourceNotFoundException ex) {

            throw ex;

        } catch (Exception ex) {

            throw new BusinessException(
                    ErrorCode.DEPENDENCY_ERROR,
                    "Không thể kiểm tra Product từ product-service"
            );
        }
    }

    private void validateCategories(
            List<String> categoryIds
    ) {

        for (String categoryId : categoryIds) {

            validateCategory(categoryId);
        }
    }

    private Category validateCategory(
            String categoryId
    ) {

        Category category =
                categoryRepository.findById(categoryId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Category không tồn tại: "
                                                + categoryId
                                )
                        );

        if (category.getStatus() != CategoryStatus.ACTIVE) {

            throw new BusinessException(
                    ErrorCode.BAD_REQUEST,
                    "Category không ở trạng thái ACTIVE: "
                            + categoryId
            );
        }

        return category;
    }

    private void validateRequest(
            ProductCategoryUpdateRequest request
    ) {

        List<String> categoryIds =
                request.categoryIds();

        if (categoryIds == null
                || categoryIds.isEmpty()) {

            throw new BusinessException(
                    ErrorCode.BAD_REQUEST,
                    "Product phải có ít nhất một category"
            );
        }

        /*
         * Không cho category bị trùng.
         */
        Set<String> uniqueCategoryIds =
                new HashSet<>(categoryIds);

        if (uniqueCategoryIds.size()
                != categoryIds.size()) {

            throw new BusinessException(
                    ErrorCode.BAD_REQUEST,
                    "Danh sách category không được trùng"
            );
        }
    }

    private void validatePrimaryCategory(
            List<String> categoryIds,
            String primaryCategoryId
    ) {

        if (!categoryIds.contains(primaryCategoryId)) {

            throw new BusinessException(
                    ErrorCode.BAD_REQUEST,
                    "Primary category phải nằm trong danh sách category"
            );
        }
    }
}