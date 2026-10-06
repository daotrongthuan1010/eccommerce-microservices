package com.dtthuan3.ecommerce.productservice.repository;

import com.dtthuan3.ecommerce.productservice.domain.ProductAttributeValue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductAttributeValueRepository
        extends JpaRepository<ProductAttributeValue, Long> {

    List<ProductAttributeValue> findByProductId(Long productId);

    Optional<ProductAttributeValue> findByProductIdAndAttributeId(
            Long productId,
            Long attributeId
    );

    boolean existsByProductIdAndAttributeId(
            Long productId,
            Long attributeId
    );

    boolean existsByProductIdAndAttributeIdAndIdNot(
            Long productId,
            Long attributeId,
            Long id
    );

    void deleteByProductId(Long productId);
}