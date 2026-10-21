package com.dtthuan3.ecommerce.catalogservice.repository;

import com.dtthuan3.ecommerce.catalogservice.entity.ProductCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductCategoryRepository
        extends JpaRepository<ProductCategory, String> {

    /**
     * Lấy toàn bộ category của product.
     */
    List<ProductCategory> findByProductId(String productId);

    /**
     * Lấy toàn bộ product thuộc category.
     */
    List<ProductCategory> findByCategoryId(String categoryId);

    /**
     * Tìm mapping product-category.
     */
    Optional<ProductCategory> findByProductIdAndCategoryId(
            String productId,
            String categoryId
    );

    /**
     * Kiểm tra mapping đã tồn tại chưa.
     */
    boolean existsByProductIdAndCategoryId(
            String productId,
            String categoryId
    );

    /**
     * Lấy primary category của product.
     */
    Optional<ProductCategory> findByProductIdAndPrimaryTrue(
            String productId
    );

    /**
     * Kiểm tra product đã có primary category chưa.
     */
    boolean existsByProductIdAndPrimaryTrue(
            String productId
    );

    /**
     * Xóa một mapping.
     */
    void deleteByProductIdAndCategoryId(
            String productId,
            String categoryId
    );

    /**
     * Xóa toàn bộ category mapping của product.
     */
    void deleteByProductId(String productId);
}