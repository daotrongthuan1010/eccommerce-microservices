package com.dtthuan3.ecommerce.productservice.repository;

import com.dtthuan3.ecommerce.productservice.domain.AttributeOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AttributeOptionRepository
        extends JpaRepository<AttributeOption, Long> {

    List<AttributeOption> findByAttributeId(Long attributeId);

    List<AttributeOption> findByAttributeIdAndActiveTrue(Long attributeId);

    Optional<AttributeOption> findByAttributeIdAndValue(
            Long attributeId,
            String value
    );

    boolean existsByAttributeIdAndValue(
            Long attributeId,
            String value
    );

    boolean existsByAttributeIdAndValueAndIdNot(
            Long attributeId,
            String value,
            Long id
    );

    void deleteByAttributeId(Long attributeId);
}