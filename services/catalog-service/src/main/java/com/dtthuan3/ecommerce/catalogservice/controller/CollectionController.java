package com.dtthuan3.ecommerce.catalogservice.controller;

import com.dtthuan3.ecommerce.catalogservice.contstant.CollectionStatus;
import com.dtthuan3.ecommerce.catalogservice.dto.request.CollectionCreateRequest;
import com.dtthuan3.ecommerce.catalogservice.dto.request.CollectionReorderRequest;
import com.dtthuan3.ecommerce.catalogservice.dto.request.CollectionUpdateRequest;
import com.dtthuan3.ecommerce.catalogservice.dto.response.CollectionResponse;
import com.dtthuan3.ecommerce.catalogservice.service.CollectionService;
import com.dtthuan3.ecommerce.common.response.ApiResponse;
import com.dtthuan3.ecommerce.common.response.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/collections")
@RequiredArgsConstructor
public class CollectionController {

    private final CollectionService collectionService;

    /**
     * Tạo collection
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CollectionResponse>> create(
            @Valid @RequestBody CollectionCreateRequest request
    ) {
        CollectionResponse response = collectionService.create(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created(response));
    }

    /**
     * Lấy collection theo ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CollectionResponse>> getById(
            @PathVariable String id
    ) {
        CollectionResponse response = collectionService.getById(id);

        return ResponseEntity.ok(
                ApiResponse.ok(response)
        );
    }

    /**
     * Danh sách collection
     *
     * Có thể filter:
     * - status
     * - keyword
     * - page
     * - size
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<CollectionResponse>>> getCollections(
            @RequestParam(required = false) CollectionStatus status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PageResponse<CollectionResponse> response =
                collectionService.getCollections(
                        status,
                        keyword,
                        page,
                        size
                );

        return ResponseEntity.ok(
                ApiResponse.ok(response)
        );
    }

    /**
     * Lấy các collection ACTIVE
     */
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<CollectionResponse>>> getActiveCollections() {

        List<CollectionResponse> response =
                collectionService.getActiveCollections();

        return ResponseEntity.ok(
                ApiResponse.ok(response)
        );
    }

    /**
     * Lấy collection đang được hiển thị trên homepage
     *
     * Service sẽ kiểm tra:
     * - ACTIVE
     * - startAt
     * - endAt
     * - displayOrder
     */
    @GetMapping("/homepage")
    public ResponseEntity<ApiResponse<List<CollectionResponse>>> getHomepageCollections() {

        List<CollectionResponse> response =
                collectionService.getHomepageCollections();

        return ResponseEntity.ok(
                ApiResponse.ok(response)
        );
    }

    /**
     * Cập nhật collection
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CollectionResponse>> update(
            @PathVariable String id,
            @Valid @RequestBody CollectionUpdateRequest request
    ) {
        CollectionResponse response =
                collectionService.update(id, request);

        return ResponseEntity.ok(
                ApiResponse.ok(response)
        );
    }

    /**
     * Cập nhật trạng thái collection
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CollectionResponse>> updateStatus(
            @PathVariable String id,
            @RequestParam CollectionStatus status
    ) {
        CollectionResponse response =
                collectionService.updateStatus(id, status);

        return ResponseEntity.ok(
                ApiResponse.ok(response)
        );
    }

    /**
     * Publish collection
     */
    @PostMapping("/{id}/publish")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CollectionResponse>> publish(
            @PathVariable String id
    ) {
        CollectionResponse response =
                collectionService.publish(id);

        return ResponseEntity.ok(
                ApiResponse.ok(response)
        );
    }

    /**
     * Unpublish collection
     */
    @PostMapping("/{id}/unpublish")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CollectionResponse>> unpublish(
            @PathVariable String id
    ) {
        CollectionResponse response =
                collectionService.unpublish(id);

        return ResponseEntity.ok(
                ApiResponse.ok(response)
        );
    }

    /**
     * Reorder collection
     */
    @PutMapping("/reorder")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> reorder(
            @Valid @RequestBody CollectionReorderRequest request
    ) {
        collectionService.reorder(request);

        return ResponseEntity.ok(
                ApiResponse.ok()
        );
    }

    /**
     * Xóa collection
     *
     * Theo business rule:
     * chỉ collection ARCHIVED mới được xóa.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable String id
    ) {
        collectionService.delete(id);

        return ResponseEntity.ok(
                ApiResponse.ok()
        );
    }
}
