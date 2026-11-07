package com.dtthuan3.ecommerce.reviewservice.controller;

import com.dtthuan3.ecommerce.common.response.ApiResponse;
import com.dtthuan3.ecommerce.common.response.PageResponse;
import com.dtthuan3.ecommerce.reviewservice.dto.request.BannedWordRequest;
import com.dtthuan3.ecommerce.reviewservice.dto.response.BannedWordResponse;
import com.dtthuan3.ecommerce.reviewservice.service.BannedWordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

/** CRUD từ cấm cho kiểm duyệt review (chỉ ADMIN). */
@RestController
@RequestMapping("/admin/reviews/banned-words")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class BannedWordAdminController {

    private final BannedWordService bannedWordService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<BannedWordResponse>>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(ApiResponse.ok(bannedWordService.search(search, page, size)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BannedWordResponse>> create(
            @Valid @RequestBody BannedWordRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String createdBy = jwt == null ? null : jwt.getSubject();
        return ResponseEntity.status(201)
                .body(ApiResponse.created(bannedWordService.create(createdBy, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        bannedWordService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /** Ép tải lại cache từ cấm (dùng khi sửa DB trực tiếp). */
    @PostMapping("/reload")
    public ResponseEntity<ApiResponse<Void>> reload() {
        bannedWordService.reload();
        return ResponseEntity.ok(ApiResponse.ok("Đã tải lại danh sách từ cấm", null));
    }
}
