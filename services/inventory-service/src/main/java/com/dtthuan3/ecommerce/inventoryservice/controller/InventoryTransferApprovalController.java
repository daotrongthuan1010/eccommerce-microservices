package com.dtthuan3.ecommerce.inventoryservice.controller;

import com.dtthuan3.ecommerce.common.response.ApiResponse;
import com.dtthuan3.ecommerce.inventoryservice.dto.request.ApproveInventoryTransferRequest;
import com.dtthuan3.ecommerce.inventoryservice.dto.response.InventoryTransferResponse;
import com.dtthuan3.ecommerce.inventoryservice.service.InventoryTransferService;
import com.dtthuan3.ecommerce.inventoryservice.service.InventoryWarehouseAuthorizationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint duyệt phiếu điều chuyển trước khi kho nguồn xác nhận xuất hàng.
 * Class này là ranh giới HTTP của Inventory: nhận path/query/body, áp dụng xác thực và kiểm tra đầu vào,
 * gọi service nghiệp vụ rồi đóng gói HTTP response; không tự thực hiện truy vấn SQL hay cập nhật tồn.
 */
@RestController
@RequestMapping("/inventory/transfers")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class InventoryTransferApprovalController {
    private final InventoryTransferService transferService;
    private final InventoryWarehouseAuthorizationService warehouseAuthorizationService;

    /** Người duyệt được lấy từ JWT; backend từ chối nếu chính người đó đã tạo phiếu. */
    @PostMapping("/{transferId}/approve")
    @PreAuthorize("hasRole('ADMIN') or hasRole('INVENTORY_APPROVE')")
    public ApiResponse<InventoryTransferResponse> approve(
            @PathVariable String transferId,
            @Valid @RequestBody ApproveInventoryTransferRequest request,
            Authentication authentication) {
        String approver = authentication == null ? null : authentication.getName();
        requireTransferWarehouseAccess(transferId);
        return ApiResponse.ok("Đã duyệt phiếu điều chuyển; kho nguồn có thể xác nhận xuất hàng",
                transferService.approve(transferId, request, approver));
    }

    /** Kiểm tra cả kho nguồn và kho đích trước khi duyệt phiếu. */
    private void requireTransferWarehouseAccess(String transferId) {
        InventoryTransferResponse transfer = transferService.getById(transferId);
        warehouseAuthorizationService.requireWarehouseAccess(transfer.sourceWarehouseId());
        warehouseAuthorizationService.requireWarehouseAccess(transfer.destinationWarehouseId());
    }
}
