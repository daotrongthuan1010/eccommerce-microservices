package com.dtthuan3.ecommerce.catalogservice.controller;

import com.dtthuan3.ecommerce.catalogservice.dto.request.CategoryRequest;
import com.dtthuan3.ecommerce.catalogservice.dto.response.CategoryResponse;
import com.dtthuan3.ecommerce.catalogservice.service.CategoryService;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/catalog/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;


    @PostMapping(
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoryResponse> create(
            @Valid @RequestBody CategoryRequest request
    ) {

        CategoryResponse response =
                categoryService.create(request, null);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }


    @GetMapping(
            value = "/{id}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CategoryResponse> getById(

            @Parameter(
                    description = "ID danh mục",
                    example = "1"
            )
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                categoryService.getById(id)
        );
    }


    @PutMapping(
            value = "/{id}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoryResponse> update(

            @Parameter(
                    description = "ID danh mục",
                    example = "1"
            )
            @PathVariable Long id,

            @Valid @RequestBody CategoryRequest request
    ) {

        return ResponseEntity.ok(
                categoryService.update(
                        id,
                        request,
                        null
                )
        );
    }


    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(

            @Parameter(
                    description = "ID danh mục",
                    example = "1"
            )
            @PathVariable Long id
    ) {

        categoryService.delete(id);

        return ResponseEntity
                .noContent()
                .build();
    }
}