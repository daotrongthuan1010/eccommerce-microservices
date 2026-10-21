package com.dtthuan3.ecommerce.inventoryservice.controller;

import com.dtthuan3.ecommerce.common.response.ApiResponse;
import com.dtthuan3.ecommerce.inventoryservice.dto.request.PostSupplierReceiptRequest;
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

/** REST adapter ghi sổ phiếu đã được duyệt và cộng số lượng thực nhận vào tồn kho. */
@RestController
@RequestMapping("/receipts")
@RequiredArgsConstructor
@Validated
public class SupplierReceiptPostingController {
    private final SupplierReceiptService receiptService;

    /**
     * Chốt một phiếu APPROVED sau khi kiểm đếm: mỗi dòng phải có Location đích.
     * Chỉ ADMIN được ghi sổ vì thao tác tạo movement và thay đổi tồn kho.
     */
    @PostMapping("/{receiptId}/post")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<SupplierReceiptResponse> post(
            @PathVariable @NotBlank @Size(max = 36) String receiptId,
            @Valid @RequestBody PostSupplierReceiptRequest request,
            Authentication authentication) {
        String performedBy = authentication == null ? null : authentication.getName();
        return ApiResponse.ok("Ghi sổ phiếu nhập và cập nhật tồn kho thành công",
                receiptService.post(receiptId, request, performedBy));
    }
}
