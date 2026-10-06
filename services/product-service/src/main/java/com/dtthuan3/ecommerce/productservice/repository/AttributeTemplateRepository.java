package com.dtthuan3.ecommerce.productservice.repository;

import com.dtthuan3.ecommerce.productservice.domain.AttributeTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AttributeTemplateRepository
        extends JpaRepository<AttributeTemplate, Long> {

    Optional<AttributeTemplate> findByCode(String code);

    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, Long id);

    List<AttributeTemplate> findByActiveTrue();
}