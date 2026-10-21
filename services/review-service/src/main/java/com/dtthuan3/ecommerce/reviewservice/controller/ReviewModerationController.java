package com.dtthuan3.ecommerce.reviewservice.controller;


import com.dtthuan3.ecommerce.common.response.ApiResponse;
import com.dtthuan3.ecommerce.reviewservice.dto.request.ModerationRequest;
import com.dtthuan3.ecommerce.reviewservice.service.ReviewModerationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/reviews")
@RequiredArgsConstructor
public class ReviewModerationController {
    private final ReviewModerationService reviewModerationService;

    @PostMapping("/{id}/moderate")
    @PreAuthorize("hasRole('MODERATOR') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> moderateReview(
            @PathVariable("id") String reviewId,
            @Valid @RequestBody ModerationRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String moderatorId = jwt.getSubject();
        reviewModerationService.moderateReview(moderatorId, reviewId, request);
        return ResponseEntity.ok(ApiResponse.ok());
    }
}
