package com.dtthuan3.ecommerce.reviewservice.controller;

import com.dtthuan3.ecommerce.common.response.ApiResponse;
import com.dtthuan3.ecommerce.reviewservice.dto.request.ReportReviewRequest;
import com.dtthuan3.ecommerce.reviewservice.dto.request.SellerReplyRequest;
import com.dtthuan3.ecommerce.reviewservice.dto.response.HelpfulResponse;
import com.dtthuan3.ecommerce.reviewservice.dto.response.ReviewResponse;
import com.dtthuan3.ecommerce.reviewservice.service.ReviewInteractionService;
import com.dtthuan3.ecommerce.reviewservice.service.ReviewModerationService;
import com.dtthuan3.ecommerce.reviewservice.dto.request.HelpfulRequest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewInteractionController {
    private final ReviewInteractionService interactionService;
    private final ReviewModerationService moderationService;

    /** Đặt trạng thái "hữu ích" mong muốn (idempotent). */
    @PutMapping("/{id}/helpful")
    public ResponseEntity<ApiResponse<HelpfulResponse>> setHelpful(
            @PathVariable("id") String reviewId,
            @Valid @RequestBody HelpfulRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResponse.ok(
                interactionService.setHelpful(jwt.getSubject(), reviewId, request.getHelpful())));
    }

    @PostMapping("/{id}/reports")
    public ResponseEntity<ApiResponse<Void>> report(
            @PathVariable("id") String reviewId,
            @Valid @RequestBody ReportReviewRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        interactionService.report(jwt.getSubject(), reviewId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(null));
    }

    /** Nhân viên CSKH phản hồi đại diện doanh nghiệp (REV-06). */
    @PostMapping("/{id}/reply")
    //@PreAuthorize(ReviewAuthorities.REPLY)
    public ResponseEntity<ApiResponse<ReviewResponse.ReviewReplyDto>> reply(
            @PathVariable("id") String reviewId,
            @Valid @RequestBody SellerReplyRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(
                moderationService.replyToReview(jwt.getSubject(), reviewId, request)));
    }

    @DeleteMapping("/{id}/reply/{replyId}")
    //@PreAuthorize(ReviewAuthorities.REPLY)
    public ResponseEntity<ApiResponse<Void>> removeReply(
            @PathVariable("id") String reviewId,
            @PathVariable String replyId,
            @AuthenticationPrincipal Jwt jwt) {
        moderationService.removeReply(jwt.getSubject(), reviewId, replyId);
        return ResponseEntity.ok(ApiResponse.ok());
    }
}