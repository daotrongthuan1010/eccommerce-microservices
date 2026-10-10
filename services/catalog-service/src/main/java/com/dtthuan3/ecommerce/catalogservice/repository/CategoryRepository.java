package com.dtthuan3.ecommerce.catalogservice.repository;


import com.dtthuan3.ecommerce.catalogservice.contstant.CategoryStatus;
import com.dtthuan3.ecommerce.catalogservice.entity.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;


public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findBySlug(
            String slug
    );

    boolean existsBySlug(
            String slug
    );

    boolean existsBySlugAndIdNot(
            String slug,
            String id
    );

    List<Category>
    findByParentIdOrderByDisplayOrderAsc(
            String parentId
    );

    List<Category>
    findByParentIdIsNullOrderByDisplayOrderAsc();

    boolean existsByParentId(
            String parentId
    );

    Page<Category>
    findByStatusOrderByDisplayOrderAsc(
            CategoryStatus status,
            Pageable pageable
    );

    @Query("""
        SELECT c
        FROM Category c
        WHERE c.status = :status
        ORDER BY
            c.parentId NULLS FIRST,
            c.displayOrder ASC,
            c.name ASC
        """)
    List<Category> findAllForTree(
            @Param("status")
            CategoryStatus status
    );}
