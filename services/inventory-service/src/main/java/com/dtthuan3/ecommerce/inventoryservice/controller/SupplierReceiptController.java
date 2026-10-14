package com.dtthuan3.ecommerce.inventoryservice.controller;

import com.dtthuan3.ecommerce.common.response.ApiResponse;
import com.dtthuan3.ecommerce.inventoryservice.dto.request.CreateSupplierReceiptRequest;
import com.dtthuan3.ecommerce.inventoryservice.dto.response.SupplierReceiptResponse;
import com.dtthuan3.ecommerce.inventoryservice.service.SupplierReceiptService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST adapter cho bước đầu của nghiệp vụ nhập nhà cung cấp.
 * Endpoint chỉ tạo phiếu nháp; cập nhật thực nhận, gửi duyệt và cộng tồn là các API riêng.
 */
@RestController
@RequestMapping("/warehouses/{warehouseId}/receipts")
@RequiredArgsConstructor
@Validated
public class SupplierReceiptController {
    private final SupplierReceiptService receiptService;

    /**
     * Mở phiếu nhập nháp cho một kho.
     * Gateway công khai đường dẫn /api/v1/warehouses/{warehouseId}/receipts và chuyển tiếp
     * request vào controller này sau khi bỏ tiền tố /api/v1.
     *
     * @param warehouseId kho phải tồn tại và đang hoạt động trong projection của Inventory
     * @param request thông tin nhà cung cấp, dòng SKU dự kiến, chứng từ và operationId
     * @return phong bì API chung chứa phiếu DRAFT mới hoặc kết quả cũ khi caller retry an toàn
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SupplierReceiptResponse> create(
            @PathVariable @NotBlank @Size(max = 100) String warehouseId,
            @Valid @RequestBody CreateSupplierReceiptRequest request) {
        return ApiResponse.created(receiptService.create(warehouseId, request));
    }
}
