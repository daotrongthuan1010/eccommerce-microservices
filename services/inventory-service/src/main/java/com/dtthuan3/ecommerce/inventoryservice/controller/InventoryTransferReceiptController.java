package com.dtthuan3.ecommerce.inventoryservice.controller;

import com.dtthuan3.ecommerce.common.response.ApiResponse;
import com.dtthuan3.ecommerce.inventoryservice.dto.request.ReceiveInventoryTransferRequest;
import com.dtthuan3.ecommerce.inventoryservice.dto.response.InventoryTransferReceiveResponse;
import com.dtthuan3.ecommerce.inventoryservice.service.InventoryTransferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Endpoint nhận hàng tại kho đích sau khi phiếu đã được dispatch. */
@RestController
@RequestMapping("/inventory/transfers")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class InventoryTransferReceiptController {
    private final InventoryTransferService transferService;

    /** Nhận từng phần hoặc toàn bộ phiếu; người xác nhận được lấy từ JWT. */
    @PostMapping("/{transferId}/receive")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<InventoryTransferReceiveResponse> receive(
            @PathVariable String transferId,
            @Valid @RequestBody ReceiveInventoryTransferRequest request,
            Authentication authentication) {
        return ApiResponse.ok("Đã ghi nhận hàng nhận tại kho đích",
                transferService.receive(transferId, request,
                        authentication == null ? null : authentication.getName()));
    }
}
