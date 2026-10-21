package com.dtthuan3.ecommerce.productservice.repository;

import com.dtthuan3.ecommerce.productservice.domain.ProductPublishingHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProductPublishingHistoryRepository
        extends JpaRepository<ProductPublishingHistory, UUID> {

    List<ProductPublishingHistory> findByProductIdOrderByChangedAtDesc(
            UUID productId
    );

    List<ProductPublishingHistory> findByRevisionIdOrderByChangedAtDesc(
            UUID revisionId
    );
}
