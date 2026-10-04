package com.dtthuan3.ecommerce.catalogservice.controller;

import com.dtthuan3.ecommerce.catalogservice.dto.request.CategoryRequest;
import com.dtthuan3.ecommerce.catalogservice.dto.response.CategoryResponse;
import com.dtthuan3.ecommerce.catalogservice.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/catalog/categories")
@RequiredArgsConstructor
@Tag(
        name = "Category",
        description = "API quản lý danh mục sản phẩm"
)
public class CategoryController {

    private final CategoryService categoryService;


    @PostMapping
    @Operation(
            summary = "Tạo danh mục"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Tạo danh mục thành công"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Dữ liệu không hợp lệ"
            )
    })
    public ResponseEntity<CategoryResponse> create(
            @Valid @RequestBody CategoryRequest request
    ) {

        CategoryResponse response =
                categoryService.create(request, null);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }




    @GetMapping("/{id}")
    @Operation(
            summary = "Lấy danh mục theo ID"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Lấy thành công"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Không tìm thấy danh mục"
            )
    })
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



    @PutMapping("/{id}")
    @Operation(
            summary = "Cập nhật danh mục"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Cập nhật thành công"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Không tìm thấy danh mục"
            )
    })
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
    @Operation(
            summary = "Xóa danh mục",
            description = "Xóa danh mục sản phẩm"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "Xóa thành công"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Không tìm thấy danh mục"
            )
    })
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