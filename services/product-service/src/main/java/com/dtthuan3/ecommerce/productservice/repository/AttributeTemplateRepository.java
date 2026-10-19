
package com.dtthuan3.ecommerce.productservice.repository;

import com.dtthuan3.ecommerce.productservice.domain.AttributeTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AttributeTemplateRepository
        extends JpaRepository<AttributeTemplate, UUID> {

    Optional<AttributeTemplate> findByIdAndActiveTrue(UUID id);

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, UUID id);
}
