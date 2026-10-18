package com.dtthuan3.ecommerce.inventoryservice.controller;

import com.dtthuan3.ecommerce.common.response.ApiResponse;
import com.dtthuan3.ecommerce.inventoryservice.dto.request.UpdateSupplierReceiptActualsRequest;
import com.dtthuan3.ecommerce.inventoryservice.dto.response.SupplierReceiptResponse;
import com.dtthuan3.ecommerce.inventoryservice.service.SupplierReceiptService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** REST adapter ghi nhận hàng thực nhận và phân loại chất lượng trên phiếu nháp. */
@RestController
@RequestMapping("/receipts")
@RequiredArgsConstructor
@Validated
public class SupplierReceiptActualsController {
    private final SupplierReceiptService receiptService;

    /**
     * Đọc một phiếu nhập gồm trạng thái, version, toàn bộ dòng hàng và chứng từ liên quan.
     * USER/ADMIN được xem; thao tác chỉ đọc nên không cập nhật tồn hoặc khóa dòng ghi.
     *
     * @param receiptId ID phiếu được trả về khi tạo phiếu nháp
     * @return dữ liệu chi tiết mới nhất; ID không tồn tại trả lỗi NOT_FOUND
     */
    @GetMapping("/{receiptId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ApiResponse<SupplierReceiptResponse> getById(
            @PathVariable @NotBlank @Size(max = 36) String receiptId) {
        return ApiResponse.ok("Lấy chi tiết phiếu nhập thành công", receiptService.getById(receiptId));
    }

    /**
     * Cập nhật số liệu thực nhận của toàn bộ dòng phiếu.
     * Endpoint chỉ cập nhật chứng từ DRAFT, chưa tạo StockLedger hay tăng StockBalance.
     *
     * @param receiptId ID phiếu nhập cần ghi nhận thực nhận
     * @param request version người dùng đã đọc, số liệu thực tế và chứng từ bằng chứng
     * @return phiếu mới nhất cùng version tăng thêm một
     */
    @PatchMapping("/{receiptId}/actuals")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<SupplierReceiptResponse> updateActuals(
            @PathVariable @NotBlank @Size(max = 36) String receiptId,
            @Valid @RequestBody UpdateSupplierReceiptActualsRequest request) {
        return ApiResponse.ok("Cập nhật số lượng thực nhận thành công",
                receiptService.updateActuals(receiptId, request));
    }
}
