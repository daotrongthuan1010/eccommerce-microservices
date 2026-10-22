package com.dtthuan3.ecommerce.productservice.repository;

import com.dtthuan3.ecommerce.productservice.domain.ProductRevision;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRevisionRepository
        extends JpaRepository<ProductRevision, UUID> {

    List<ProductRevision> findByProductId(UUID productId);

    Optional<ProductRevision> findFirstByProductIdOrderByVersionDesc(
            UUID productId
    );

    boolean existsByProductIdAndVersion(
            UUID productId,
            Long version
    );

    Optional<ProductRevision> findByIdAndProductId(
            UUID id,
            UUID productId
    );
}
