package com.dtthuan3.ecommerce.inventoryservice.controller;

import com.dtthuan3.ecommerce.common.response.ApiResponse;
import com.dtthuan3.ecommerce.common.response.PageResponse;
import com.dtthuan3.ecommerce.inventoryservice.dto.response.InventoryTransferResponse;
import com.dtthuan3.ecommerce.inventoryservice.service.InventoryTransferService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** API truy vấn phiếu điều chuyển để đọc trạng thái và version trước thao tác tiếp theo. */
@RestController
@RequestMapping("/inventory/transfers")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class InventoryTransferQueryController {
    private final InventoryTransferService transferService;

    /** Danh sách phiếu của một doanh nghiệp, có thể lọc trạng thái và tối đa 100 bản ghi mỗi trang. */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PageResponse<InventoryTransferResponse>> list(
            @RequestParam String enterpriseId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<InventoryTransferResponse> result = transferService.list(enterpriseId, status,
                PageRequest.of(Math.max(page, 0), Math.max(1, Math.min(size, 100))));
        return ApiResponse.ok("Lấy danh sách phiếu điều chuyển thành công",
                PageResponse.of(result.getContent(), result.getNumber(), result.getSize(),
                        result.getTotalElements(), result.getTotalPages()));
    }

    /** Trả header và các dòng của phiếu; endpoint chỉ đọc nên không khóa tồn kho. */
    @GetMapping("/{transferId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<InventoryTransferResponse> getById(@PathVariable String transferId) {
        return ApiResponse.ok("Lấy chi tiết phiếu điều chuyển thành công",
                transferService.getById(transferId));
    }
}
