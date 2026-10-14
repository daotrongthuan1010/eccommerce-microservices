package com.dtthuan3.ecommerce.catalogservice.repository;


import com.dtthuan3.ecommerce.catalogservice.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;


public interface CategoryRepository extends JpaRepository<Category, Long> {

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(
            String slug,
            Long id
    );

    boolean existsByParentId(Long parentId);
}
