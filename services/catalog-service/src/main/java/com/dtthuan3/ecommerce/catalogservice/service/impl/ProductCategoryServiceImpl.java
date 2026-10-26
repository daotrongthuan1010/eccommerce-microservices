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
import com.dtthuan3.ecommerce.catalogservice.service.ProductCategoryService;
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
        implements ProductCategoryService {

    private final ProductCategoryRepository productCategoryRepository;

    private final CategoryRepository categoryRepository;

    private final ProductCategoryMapper productCategoryMapper;

    private final ProductServiceClient productServiceClient;


    // =========================================================
    // GET PRODUCT CATEGORIES
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public ProductCategoriesResponse getProductCategories(
            UUID productId
    ) {

        validateProductExists(productId);

        List<ProductCategory> mappings =
                productCategoryRepository.findByProductId(productId);

        return buildProductCategoriesResponse(
                productId,
                mappings
        );
    }


    // =========================================================
    // UPDATE PRODUCT CATEGORIES
    // =========================================================

    @Override
    @Transactional
    public ProductCategoriesResponse updateProductCategories(
            UUID productId,
            ProductCategoryUpdateRequest request
    ) {

        /*
         * 1. Kiểm tra Product tồn tại.
         */
        validateProductExists(productId);

        /*
         * 2. Validate request.
         */
        validateRequest(request);

        List<String> categoryIds =
                request.categoryIds();

        String primaryCategoryId =
                request.primaryCategoryId();

        /*
         * 3. Primary bắt buộc nằm trong categoryIds.
         */
        validatePrimaryCategory(
                categoryIds,
                primaryCategoryId
        );

        /*
         * 4. Tất cả category phải tồn tại
         *    và đang ACTIVE.
         */
        validateCategories(categoryIds);

        /*
         * 5. Lấy mapping hiện tại.
         */
        List<ProductCategory> existingMappings =
                productCategoryRepository.findByProductId(productId);

        Map<String, ProductCategory> existingMap =
                existingMappings.stream()
                        .collect(Collectors.toMap(
                                ProductCategory::getCategoryId,
                                Function.identity()
                        ));

        /*
         * 6. Xóa mapping không còn nằm trong request.
         */
        for (ProductCategory existing : existingMappings) {

            if (!categoryIds.contains(
                    existing.getCategoryId()
            )) {

                productCategoryRepository.delete(existing);
            }
        }

        /*
         * 7. Tạo mới / cập nhật mapping.
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

        /*
         * 8. Đọc lại từ DB.
         *
         * Không gọi validateProductExists()
         * lần thứ hai.
         */
        List<ProductCategory> updatedMappings =
                productCategoryRepository.findByProductId(productId);

        return buildProductCategoriesResponse(
                productId,
                updatedMappings
        );
    }


    // =========================================================
    // SET PRIMARY CATEGORY
    // =========================================================

    @Override
    @Transactional
    public ProductCategoryResponse setPrimaryCategory(
            UUID productId,
            String categoryId
    ) {

        /*
         * 1. Product phải tồn tại.
         */
        validateProductExists(productId);

        /*
         * 2. Category phải tồn tại + ACTIVE.
         */
        validateCategory(categoryId);

        /*
         * 3. Product phải đang được gán
         *    vào category này.
         */
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

        /*
         * 4. Tìm primary hiện tại.
         */
        Optional<ProductCategory> currentPrimary =
                productCategoryRepository
                        .findByProductIdAndPrimaryTrue(
                                productId
                        );

        /*
         * 5. Bỏ primary cũ.
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
         * 6. Set primary mới.
         */
        mapping.setPrimary(true);

        productCategoryRepository.save(mapping);

        return productCategoryMapper.toResponse(
                mapping
        );
    }


    // =========================================================
    // REMOVE PRODUCT FROM CATEGORY
    // =========================================================

    @Override
    @Transactional
    public void removeProductFromCategory(
            UUID productId,
            String categoryId
    ) {

        /*
         * 1. Product phải tồn tại.
         */
        validateProductExists(productId);

        /*
         * 2. Mapping phải tồn tại.
         */
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
         * 3. Nếu xóa primary mà Product còn
         *    category khác thì bắt buộc chọn primary mới.
         */
        if (Boolean.TRUE.equals(
                mapping.getPrimary()
        )) {

            long count =
                    productCategoryRepository
                            .countByProductId(productId);

            if (count > 1) {

                throw new BusinessException(
                        ErrorCode.BAD_REQUEST,
                        "Không thể xóa primary category. " +
                                "Hãy chọn primary category khác trước"
                );
            }
        }

        /*
         * 4. Xóa mapping.
         */
        productCategoryRepository.delete(mapping);
    }


    // =========================================================
    // GET CATEGORY PRODUCTS
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public CategoryProductsResponse getCategoryProducts(
            String categoryId
    ) {

        /*
         * Category phải tồn tại.
         */
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
    // BUILD RESPONSE
    // =========================================================

    private ProductCategoriesResponse
    buildProductCategoriesResponse(
            UUID productId,
            List<ProductCategory> mappings
    ) {

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
                .primaryCategoryId(
                        primaryCategoryId
                )
                .categories(responses)
                .build();
    }


    // =========================================================
    // PRODUCT VALIDATION
    // =========================================================

    private void validateProductExists(
            UUID productId
    ) {

        if (productId == null) {

            throw new BusinessException(
                    ErrorCode.BAD_REQUEST,
                    "Product id không được null"
            );
        }

        try {

            ApiResponse<ProductExistsResponse> response =
                    productServiceClient.getById(productId);

            /*
             * Product Service trả response nhưng
             * không có data -> Product không tồn tại.
             */
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

            /*
             * Không được coi lỗi Product Service
             * là Product không tồn tại.
             *
             * Đây là lỗi dependency.
             */
            throw new BusinessException(
                    ErrorCode.DEPENDENCY_ERROR,
                    "Không thể kiểm tra Product từ product-service"
            );
        }
    }


    // =========================================================
    // CATEGORY VALIDATION
    // =========================================================

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

        if (categoryId == null
                || categoryId.isBlank()) {

            throw new BusinessException(
                    ErrorCode.BAD_REQUEST,
                    "Category id không được để trống"
            );
        }

        Category category =
                categoryRepository.findById(categoryId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Category không tồn tại: "
                                                + categoryId
                                )
                        );

        if (category.getStatus()
                != CategoryStatus.ACTIVE) {

            throw new BusinessException(
                    ErrorCode.BAD_REQUEST,
                    "Category không ở trạng thái ACTIVE: "
                            + categoryId
            );
        }

        return category;
    }


    // =========================================================
    // REQUEST VALIDATION
    // =========================================================

    private void validateRequest(
            ProductCategoryUpdateRequest request
    ) {

        if (request == null) {

            throw new BusinessException(
                    ErrorCode.BAD_REQUEST,
                    "Request không được null"
            );
        }

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


    // =========================================================
    // PRIMARY CATEGORY VALIDATION
    // =========================================================

    private void validatePrimaryCategory(
            List<String> categoryIds,
            String primaryCategoryId
    ) {

        if (primaryCategoryId == null
                || primaryCategoryId.isBlank()) {

            throw new BusinessException(
                    ErrorCode.BAD_REQUEST,
                    "Primary category id không được để trống"
            );
        }

        if (!categoryIds.contains(
                primaryCategoryId
        )) {

            throw new BusinessException(
                    ErrorCode.BAD_REQUEST,
                    "Primary category phải nằm trong danh sách category"
            );
        }
    }
}