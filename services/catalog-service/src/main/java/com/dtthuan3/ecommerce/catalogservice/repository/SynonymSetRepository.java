package com.dtthuan3.ecommerce.catalogservice.repository;

import com.dtthuan3.ecommerce.catalogservice.contstant.SynonymSetStatus;
import com.dtthuan3.ecommerce.catalogservice.entity.SynonymSet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface SynonymSetRepository
        extends JpaRepository<SynonymSet, String> {

    Optional<SynonymSet> findByName(String name);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(
            String name,
            String id
    );

    Page<SynonymSet> findByStatus(
            SynonymSetStatus status,
            Pageable pageable
    );

    List<SynonymSet> findAllByStatus(SynonymSetStatus status);
}
