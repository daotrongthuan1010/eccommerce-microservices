package com.dtthuan3.ecommerce.catalogservice.controller;

import com.dtthuan3.ecommerce.catalogservice.dto.request.CategoryCreateRequest;
import com.dtthuan3.ecommerce.catalogservice.dto.request.CategoryUpdateRequest;
import com.dtthuan3.ecommerce.catalogservice.dto.response.CategoryResponse;
import com.dtthuan3.ecommerce.catalogservice.dto.response.CategoryTreeResponse;
import com.dtthuan3.ecommerce.catalogservice.service.CategoryService;
import com.dtthuan3.ecommerce.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getRoots() {

        return ResponseEntity.ok(
                ApiResponse.ok(
                        categoryService.getRootCategories()
                )
        );
    }

    @GetMapping("/tree")
    public ResponseEntity<ApiResponse<List<CategoryTreeResponse>>> getTree() {

        return ResponseEntity.ok(
                ApiResponse.ok(
                        categoryService.getTree()
                )
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> getById(
            @PathVariable String id
    ) {

        return ResponseEntity.ok(
                ApiResponse.ok(
                        categoryService.getById(id)
                )
        );
    }

    @GetMapping("/{parentId}/children")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getChildren(
            @PathVariable String parentId
    ) {

        return ResponseEntity.ok(
                ApiResponse.ok(
                        categoryService.getChildren(parentId)
                )
        );
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CATALOG_MANAGER')")
    public ResponseEntity<ApiResponse<CategoryResponse>> create(
            @Valid @RequestBody CategoryCreateRequest request
    ) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.created(
                                categoryService.create(request)
                        )
                );
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CATALOG_MANAGER')")
    public ResponseEntity<ApiResponse<CategoryResponse>> update(
            @PathVariable String id,
            @Valid @RequestBody CategoryUpdateRequest request
    ) {

        return ResponseEntity.ok(
                ApiResponse.ok(
                        categoryService.update(id, request)
                )
        );
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable String id
    ) {

        categoryService.delete(id);

        return ResponseEntity.ok(
                ApiResponse.ok()
        );
    }
}