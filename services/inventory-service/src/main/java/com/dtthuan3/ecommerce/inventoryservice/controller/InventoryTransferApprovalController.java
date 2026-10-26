package com.dtthuan3.ecommerce.inventoryservice.controller;

import com.dtthuan3.ecommerce.common.response.ApiResponse;
import com.dtthuan3.ecommerce.inventoryservice.dto.request.ApproveInventoryTransferRequest;
import com.dtthuan3.ecommerce.inventoryservice.dto.response.InventoryTransferResponse;
import com.dtthuan3.ecommerce.inventoryservice.service.InventoryTransferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Endpoint duyệt phiếu điều chuyển trước khi kho nguồn xác nhận xuất hàng. */
@RestController
@RequestMapping("/inventory/transfers")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class InventoryTransferApprovalController {
    private final InventoryTransferService transferService;

    /** Người duyệt được lấy từ JWT; backend từ chối nếu chính người đó đã tạo phiếu. */
    @PostMapping("/{transferId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<InventoryTransferResponse> approve(
            @PathVariable String transferId,
            @Valid @RequestBody ApproveInventoryTransferRequest request,
            Authentication authentication) {
        String approver = authentication == null ? null : authentication.getName();
        return ApiResponse.ok("Đã duyệt phiếu điều chuyển; kho nguồn có thể xác nhận xuất hàng",
                transferService.approve(transferId, request, approver));
    }
}
