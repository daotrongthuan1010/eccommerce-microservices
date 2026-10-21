
package com.dtthuan3.ecommerce.productservice.repository;

import com.dtthuan3.ecommerce.productservice.domain.ProductMedia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProductMediaRepository
        extends JpaRepository<ProductMedia, UUID> {

    List<ProductMedia> findByProductId(UUID productId);

    List<ProductMedia> findByProductIdOrderByDisplayOrderAsc(
            UUID productId
    );

    List<ProductMedia> findByProductIdAndPrimaryTrue(UUID productId);

    void deleteByProductId(UUID productId);
}
