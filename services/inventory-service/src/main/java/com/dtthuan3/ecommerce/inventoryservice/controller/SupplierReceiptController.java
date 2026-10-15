package com.dtthuan3.ecommerce.inventoryservice.controller;

import com.dtthuan3.ecommerce.common.response.ApiResponse;
import com.dtthuan3.ecommerce.common.response.PageResponse;
import com.dtthuan3.ecommerce.inventoryservice.dto.request.CreateSupplierReceiptRequest;
import com.dtthuan3.ecommerce.inventoryservice.dto.response.SupplierReceiptResponse;
import com.dtthuan3.ecommerce.inventoryservice.dto.response.SupplierReceiptSummaryResponse;
import com.dtthuan3.ecommerce.inventoryservice.service.SupplierReceiptService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
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
     * Liệt kê phiếu nhập thuộc một kho theo trang mới nhất trước.
     * Mỗi phần tử chỉ có thông tin tóm tắt và số dòng; người dùng gọi GET chi tiết theo ID
     * khi cần xem SKU, số lượng và chứng từ, tránh phản hồi quá lớn khi kho có nhiều phiếu.
     *
     * @param warehouseId kho cần tra cứu
     * @param page số trang bắt đầu từ 0; giá trị âm được đưa về 0
     * @param size số phần tử mỗi trang; được giới hạn tối đa 100
     * @return danh sách tóm tắt cùng tổng số dòng và tổng số trang
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ApiResponse<PageResponse<SupplierReceiptSummaryResponse>> listByWarehouse(
            @PathVariable @NotBlank @Size(max = 100) String warehouseId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageable = PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, 100)));
        Page<SupplierReceiptSummaryResponse> result = receiptService.listByWarehouse(warehouseId, pageable);
        PageResponse<SupplierReceiptSummaryResponse> response = PageResponse.of(result.getContent(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
        return ApiResponse.ok("Lấy danh sách phiếu nhập thành công", response);
    }

    /**
     * Mở phiếu nhập nháp cho một kho.
     * Gateway công khai đường dẫn /api/v1/warehouses/{warehouseId}/receipts và chuyển tiếp
     * request vào controller này sau khi bỏ tiền tố /api/v1.
     *
     * @param warehouseId kho phải tồn tại và đang hoạt động trong Inventory
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
