package com.dtthuan3.ecommerce.catalogservice.mapper;

import com.dtthuan3.ecommerce.catalogservice.dto.request.CategoryCreateRequest;
import com.dtthuan3.ecommerce.catalogservice.dto.request.CategoryUpdateRequest;
import com.dtthuan3.ecommerce.catalogservice.dto.response.CategoryResponse;
import com.dtthuan3.ecommerce.catalogservice.entity.Category;
import org.springframework.stereotype.Component;

@Component
public class CategoryMapper {

    public Category toEntity(
            CategoryCreateRequest request
    ) {

        Category category = new Category();

        category.setParentId(
                request.getParentId()
        );

        category.setName(
                request.getName()
        );

        category.setSlug(
                request.getSlug()
        );

        category.setDescription(
                request.getDescription()
        );

        category.setDisplayOrder(
                request.getDisplayOrder()
        );

        category.setStatus(
                request.getStatus()
        );

        category.setRevision(0L);

        return category;
    }

    public void update(
            Category category,
            CategoryUpdateRequest request
    ) {

        category.setParentId(
                request.getParentId()
        );

        category.setName(
                request.getName()
        );

        category.setSlug(
                request.getSlug()
        );

        category.setDescription(
                request.getDescription()
        );

        category.setDisplayOrder(
                request.getDisplayOrder()
        );

        category.setStatus(
                request.getStatus()
        );
    }

    public CategoryResponse toResponse(
            Category category
    ) {

        return CategoryResponse.builder()
                .id(category.getId())
                .parentId(category.getParentId())
                .name(category.getName())
                .slug(category.getSlug())
                .description(category.getDescription())
                .imageObjectKey(
                        category.getImageObjectKey()
                )
                .displayOrder(
                        category.getDisplayOrder()
                )
                .status(category.getStatus())
                .revision(category.getRevision())
                .build();
    }
}