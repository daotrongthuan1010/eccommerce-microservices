package com.dtthuan3.ecommerce.catalogservice.mapper;

import com.dtthuan3.ecommerce.catalogservice.dto.request.CategoryRequest;
import com.dtthuan3.ecommerce.catalogservice.dto.response.CategoryResponse;
import com.dtthuan3.ecommerce.catalogservice.entity.Category;
import org.springframework.stereotype.Component;

@Component
public class CategoryMapper {

    public Category toEntity(CategoryRequest request) {

        Category category = new Category();

        category.setParentId(request.getParentId());
        category.setName(request.getName());
        category.setSlug(request.getSlug());
        category.setDescription(request.getDescription());
        category.setDisplayOrder(request.getDisplayOrder());
        category.setStatus(request.getStatus());

        return category;
    }

    public void updateEntity(
            Category category,
            CategoryRequest request
    ) {

        category.setParentId(request.getParentId());
        category.setName(request.getName());
        category.setSlug(request.getSlug());
        category.setDescription(request.getDescription());
        category.setDisplayOrder(request.getDisplayOrder());
        category.setStatus(request.getStatus());
    }

    public CategoryResponse toResponse(
            Category category,
            String imageUrl
    ) {

        return CategoryResponse.builder()
                .id(category.getId())
                .parentId(category.getParentId())
                .name(category.getName())
                .slug(category.getSlug())
                .imageUrl(imageUrl)
                .displayOrder(category.getDisplayOrder())
                .status(category.getStatus())
                .build();
    }

}