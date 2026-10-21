package com.dtthuan3.ecommerce.inventoryservice.controller;

import com.dtthuan3.ecommerce.common.response.ApiResponse;
import com.dtthuan3.ecommerce.inventoryservice.dto.request.DispatchInventoryTransferRequest;
import com.dtthuan3.ecommerce.inventoryservice.dto.response.InventoryTransferDispatchResponse;
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

/** API xác nhận kho nguồn đã bàn giao hàng và chuyển phiếu sang trạng thái IN_TRANSIT. */
@RestController
@RequestMapping("/inventory/transfers")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class InventoryTransferDispatchController {
    private final InventoryTransferService transferService;

    /**
     * Xuất toàn bộ dòng của phiếu trong một transaction; chỉ role ADMIN được phép thực hiện.
     * Người dispatch lấy từ JWT, không lấy từ request body.
     */
    @PostMapping("/{transferId}/dispatch")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<InventoryTransferDispatchResponse> dispatch(
            @PathVariable String transferId,
            @Valid @RequestBody DispatchInventoryTransferRequest request,
            Authentication authentication) {
        return ApiResponse.ok("Đã xuất chuyển; hàng đang ở trạng thái IN_TRANSIT",
                transferService.dispatch(transferId, request,
                        authentication == null ? null : authentication.getName()));
    }
}
