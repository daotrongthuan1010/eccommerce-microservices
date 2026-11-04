package com.dtthuan3.ecommerce.inventoryservice.controller;

import com.dtthuan3.ecommerce.common.response.ApiResponse;
import com.dtthuan3.ecommerce.inventoryservice.dto.request.PostSupplierReceiptRequest;
import com.dtthuan3.ecommerce.inventoryservice.dto.response.SupplierReceiptResponse;
import com.dtthuan3.ecommerce.inventoryservice.service.SupplierReceiptService;
import com.dtthuan3.ecommerce.inventoryservice.service.InventoryWarehouseAuthorizationService;
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

/**
 * REST adapter ghi sổ phiếu đã được duyệt và cộng số lượng thực nhận vào tồn kho.
 * Class này là ranh giới HTTP của Inventory: nhận path/query/body, áp dụng xác thực và kiểm tra đầu vào,
 * gọi service nghiệp vụ rồi đóng gói HTTP response; không tự thực hiện truy vấn SQL hay cập nhật tồn.
 */
@RestController
@RequestMapping("/receipts")
@RequiredArgsConstructor
@Validated
public class SupplierReceiptPostingController {
    private final SupplierReceiptService receiptService;
    private final InventoryWarehouseAuthorizationService warehouseAuthorizationService;

    /**
     * Chốt một phiếu APPROVED sau khi kiểm đếm: mỗi dòng phải có Location đích.
     * Chỉ ADMIN được ghi sổ vì thao tác tạo movement và thay đổi tồn kho.
     */
    @PostMapping("/{receiptId}/post")
    @PreAuthorize("hasRole('ADMIN') or hasRole('INVENTORY_POST')")
    public ApiResponse<SupplierReceiptResponse> post(
            @PathVariable @NotBlank @Size(max = 36) String receiptId,
            @Valid @RequestBody PostSupplierReceiptRequest request,
            Authentication authentication) {
        String performedBy = authentication == null ? null : authentication.getName();
        warehouseAuthorizationService.requireWarehouseAccess(receiptService.getById(receiptId).warehouseId());
        return ApiResponse.ok("Ghi sổ phiếu nhập và cập nhật tồn kho thành công",
                receiptService.post(receiptId, request, performedBy));
    }
}
