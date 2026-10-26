package com.dtthuan3.ecommerce.inventoryservice.controller;

import com.dtthuan3.ecommerce.common.response.ApiResponse;
import com.dtthuan3.ecommerce.inventoryservice.dto.request.ApproveSupplierReceiptRequest;
import com.dtthuan3.ecommerce.inventoryservice.dto.request.RejectSupplierReceiptRequest;
import com.dtthuan3.ecommerce.inventoryservice.dto.response.SupplierReceiptResponse;
import com.dtthuan3.ecommerce.inventoryservice.service.SupplierReceiptService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** REST adapter duyệt phiếu nhập đã được gửi sang trạng thái chờ duyệt. */
@RestController
@RequestMapping("/receipts")
@RequiredArgsConstructor
@Validated
public class SupplierReceiptApprovalController {
    private final SupplierReceiptService receiptService;

    /**
     * Chuyển phiếu SUBMITTED sang APPROVED, ghi nhận người duyệt và thời điểm duyệt.
     * Thao tác này chưa cộng tồn kho; bước ghi sổ được thực hiện riêng sau đó.
     */
    @PostMapping("/{receiptId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<SupplierReceiptResponse> approve(
            @PathVariable @NotBlank @Size(max = 36) String receiptId,
            @Valid @RequestBody ApproveSupplierReceiptRequest request,
            Authentication authentication) {
        return ApiResponse.ok("Duyệt phiếu nhập thành công",
                receiptService.approve(receiptId, request, authentication.getName()));
    }

    /** Từ chối phiếu SUBMITTED; tài khoản xử lý được lấy từ JWT, không nhận từ request body. */
    @PostMapping("/{receiptId}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<SupplierReceiptResponse> reject(
            @PathVariable @NotBlank @Size(max = 36) String receiptId,
            @Valid @RequestBody RejectSupplierReceiptRequest request,
            Authentication authentication) {
        return ApiResponse.ok("Từ chối phiếu nhập thành công",
                receiptService.reject(receiptId, request, authentication.getName()));
    }
}
