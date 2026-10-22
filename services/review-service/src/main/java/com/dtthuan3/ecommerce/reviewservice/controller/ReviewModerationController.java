package com.dtthuan3.ecommerce.reviewservice.controller;


import com.dtthuan3.ecommerce.common.response.ApiResponse;
import com.dtthuan3.ecommerce.common.response.PageResponse;
import com.dtthuan3.ecommerce.reviewservice.dto.request.ModerationActionRequest;
import com.dtthuan3.ecommerce.reviewservice.dto.request.ModerationRequest;
import com.dtthuan3.ecommerce.reviewservice.dto.response.AbuseReportResponse;
import com.dtthuan3.ecommerce.reviewservice.dto.response.ReconcileResponse;
import com.dtthuan3.ecommerce.reviewservice.dto.response.ReviewResponse;
import com.dtthuan3.ecommerce.reviewservice.service.ReviewModerationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/reviews/moderation")
@RequiredArgsConstructor
public class ReviewModerationController {
    private final ReviewModerationService moderationService;

    @GetMapping("/revisions")
    public ResponseEntity<ApiResponse<PageResponse<ReviewResponse>>> pendingQueue(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(moderationService.getPendingQueue(page, size)));
    }
    @PostMapping("/revisions/{revisionId}/decision")
    public ResponseEntity<ApiResponse<Void>> decide(
            @PathVariable String revisionId,
            @Valid @RequestBody ModerationRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        moderationService.decideRevision(jwt.getSubject(), revisionId, request);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @PostMapping("/reviews/{id}/hide")
    public ResponseEntity<ApiResponse<Void>> hide(
            @PathVariable("id") String reviewId,
            @Valid @RequestBody ModerationActionRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        moderationService.hideReview(jwt.getSubject(), reviewId, request);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @PostMapping("/reviews/{id}/restore")
    public ResponseEntity<ApiResponse<Void>> restore(
            @PathVariable("id") String reviewId,
            @Valid @RequestBody ModerationActionRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        moderationService.restoreReview(jwt.getSubject(), reviewId, request);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @GetMapping("/reports")
    public ResponseEntity<ApiResponse<PageResponse<AbuseReportResponse>>> reports(
            @RequestParam(defaultValue = "OPEN") String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(moderationService.getReports(status, page, size)));
    }

    @PostMapping("/reports/{id}/dismiss")
    public ResponseEntity<ApiResponse<Void>> dismiss(@PathVariable("id") String reportId,
                                                     @AuthenticationPrincipal Jwt jwt) {
        moderationService.dismissReport(jwt.getSubject(), reportId);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @PostMapping("/aggregates/{productId}/reconcile")
    public ResponseEntity<ApiResponse<ReconcileResponse>> reconcile(
            @PathVariable String productId,
            @RequestParam(defaultValue = "false") boolean repair) {
        return ResponseEntity.ok(ApiResponse.ok(moderationService.reconcile(productId, repair)));
    }
}