package com.dtthuan3.ecommerce.reviewservice.controller;

import com.dtthuan3.ecommerce.common.response.ApiResponse;
import com.dtthuan3.ecommerce.reviewservice.service.ReviewInteractionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewInteractionController {

    private final ReviewInteractionService interactionService;

    @PostMapping("/{id}/vote")
    public ResponseEntity<ApiResponse<Void>> voteReview(
            @PathVariable("id") String reviewId,
            @RequestParam("isHelpful") boolean isHelpful,
            @AuthenticationPrincipal Jwt jwt) {

        interactionService.voteReview(jwt.getSubject(), reviewId, isHelpful);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @DeleteMapping("/{id}/vote")
    public ResponseEntity<ApiResponse<Void>> removeVote(
            @PathVariable("id") String reviewId,
            @AuthenticationPrincipal Jwt jwt) {

        interactionService.removeVote(jwt.getSubject(), reviewId);
        return ResponseEntity.ok(ApiResponse.ok());
    }
}
