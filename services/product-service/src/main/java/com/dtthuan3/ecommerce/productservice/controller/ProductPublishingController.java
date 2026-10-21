
package com.dtthuan3.ecommerce.productservice.controller;

import com.dtthuan3.ecommerce.common.response.ApiResponse;
import com.dtthuan3.ecommerce.productservice.dto.request.PublicationDecisionRequest;
import com.dtthuan3.ecommerce.productservice.dto.response.ProductPublishingHistoryResponse;
import com.dtthuan3.ecommerce.productservice.service.ProductPublishingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ProductPublishingController {

    private final ProductPublishingService productPublishingService;

    @PostMapping(
            "/products/{productId}/revisions/{revisionId}/submit-for-approval"
    )
    public ApiResponse<UUID> submitForApproval(
            @PathVariable UUID productId,
            @PathVariable UUID revisionId,
            @RequestHeader("X-Actor-Id") UUID actorId
    ) {
        UUID approvalId = productPublishingService.submitForApproval(
                productId,
                revisionId,
                actorId
        );

        return ApiResponse.created(approvalId);
    }

    @PostMapping("/publication-approvals/{approvalId}/approve")
    public ApiResponse<Void> approve(
            @PathVariable UUID approvalId,
            @RequestHeader("X-Actor-Id") UUID actorId
    ) {
        productPublishingService.approve(approvalId, actorId);
        return ApiResponse.ok();
    }

    @PostMapping("/publication-approvals/{approvalId}/reject")
    public ApiResponse<Void> reject(
            @PathVariable UUID approvalId,
            @RequestHeader("X-Actor-Id") UUID actorId,
            @Valid @RequestBody PublicationDecisionRequest request
    ) {
        productPublishingService.reject(
                approvalId,
                actorId,
                request.reason()
        );

        return ApiResponse.ok();
    }

    @GetMapping("/products/{productId}/publishing-history")
    public ApiResponse<List<ProductPublishingHistoryResponse>> getHistory(
            @PathVariable UUID productId
    ) {
        return ApiResponse.list(
                productPublishingService.getPublishingHistory(productId)
        );
    }
}
