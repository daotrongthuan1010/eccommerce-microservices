package com.dtthuan3.ecommerce.catalogservice.repository;

import com.dtthuan3.ecommerce.catalogservice.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository
        extends JpaRepository<Category, String> {

    Optional<Category> findById(String id);

    boolean existsById(String id);

    boolean existsByParentId(String parentId);

    boolean existsBySlugAndParentId(
            String slug,
            String parentId
    );

    boolean existsBySlugAndParentIdAndIdNot(
            String slug,
            String parentId,
            String id
    );

    List<Category> findByParentIdOrderByDisplayOrderAsc(
            String parentId
    );

    List<Category> findByParentIdIsNullOrderByDisplayOrderAsc();

    @Query("""
        SELECT c
        FROM Category c
        ORDER BY
            CASE WHEN c.parentId IS NULL THEN 0 ELSE 1 END,
            c.parentId,
            c.displayOrder,
            c.name
    """)
    List<Category> findAllForTree();

    @Query("""
        SELECT c.id
        FROM Category c
        WHERE c.parentId = :parentId
    """)
    List<String> findChildIds(
            @Param("parentId") String parentId
    );
}