package com.dtthuan3.ecommerce.productservice.repository;

import com.dtthuan3.ecommerce.productservice.domain.ProductMedia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductMediaRepository
        extends JpaRepository<ProductMedia, Long> {

    List<ProductMedia> findByProductId(Long productId);

    List<ProductMedia> findByProductIdOrderByDisplayOrderAsc(
            Long productId
    );

    List<ProductMedia> findByProductIdAndPrimaryTrue(
            Long productId
    );

    void deleteByProductId(Long productId);
}