package com.dtthuan3.ecommerce.productservice.repository;

import com.dtthuan3.ecommerce.productservice.domain.ProductPublishingHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductPublishingHistoryRepository
        extends JpaRepository<ProductPublishingHistory, Long> {

    List<ProductPublishingHistory> findByProductIdOrderByChangedAtDesc(
            Long productId
    );
}