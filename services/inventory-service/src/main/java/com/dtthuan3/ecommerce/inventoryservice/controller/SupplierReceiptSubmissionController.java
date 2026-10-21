package com.dtthuan3.ecommerce.inventoryservice.controller;

import com.dtthuan3.ecommerce.common.response.ApiResponse;
import com.dtthuan3.ecommerce.inventoryservice.dto.request.SubmitSupplierReceiptRequest;
import com.dtthuan3.ecommerce.inventoryservice.dto.response.SupplierReceiptResponse;
import com.dtthuan3.ecommerce.inventoryservice.service.SupplierReceiptService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** REST adapter gửi phiếu nhập đã kiểm đếm sang trạng thái chờ duyệt. */
@RestController
@RequestMapping("/receipts")
@RequiredArgsConstructor
@Validated
public class SupplierReceiptSubmissionController {
    private final SupplierReceiptService receiptService;

    /**
     * Gửi một phiếu DRAFT sang SUBMITTED sau khi kiểm tra version, kết quả kiểm đếm và chứng từ.
     * Endpoint chỉ tạo trạng thái chờ duyệt; chưa cộng tồn kho.
     */
    @PostMapping("/{receiptId}/submit")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<SupplierReceiptResponse> submit(
            @PathVariable @NotBlank @Size(max = 36) String receiptId,
            @Valid @RequestBody SubmitSupplierReceiptRequest request,
            Authentication authentication) {
        String bearerToken = authentication instanceof JwtAuthenticationToken jwt
                ? jwt.getToken().getTokenValue() : null;
        return ApiResponse.ok("Gửi phiếu nhập sang trạng thái chờ duyệt thành công",
                receiptService.submit(receiptId, request, bearerToken));
    }
}
