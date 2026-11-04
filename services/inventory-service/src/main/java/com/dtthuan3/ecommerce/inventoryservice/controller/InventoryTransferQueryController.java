package com.dtthuan3.ecommerce.inventoryservice.controller;

import com.dtthuan3.ecommerce.common.response.ApiResponse;
import com.dtthuan3.ecommerce.common.response.PageResponse;
import com.dtthuan3.ecommerce.inventoryservice.dto.response.InventoryTransferResponse;
import com.dtthuan3.ecommerce.inventoryservice.service.InventoryTransferService;
import com.dtthuan3.ecommerce.inventoryservice.service.InventoryWarehouseAuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API truy vấn phiếu điều chuyển để đọc trạng thái và version trước thao tác tiếp theo.
 * Class này là ranh giới HTTP của Inventory: nhận path/query/body, áp dụng xác thực và kiểm tra đầu vào,
 * gọi service nghiệp vụ rồi đóng gói HTTP response; không tự thực hiện truy vấn SQL hay cập nhật tồn.
 */
@RestController
@RequestMapping("/inventory/transfers")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class InventoryTransferQueryController {

    @Value("${inventory.api.transfer-page-size}")
    private int transferPageSize;
    private final InventoryTransferService transferService;
    private final InventoryWarehouseAuthorizationService warehouseAuthorizationService;

    /** Danh sách phiếu của một doanh nghiệp, có thể lọc trạng thái và tối đa 100 bản ghi mỗi trang. */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('INVENTORY_READ_ALL')")
    public ApiResponse<PageResponse<InventoryTransferResponse>> list(
            @RequestParam String enterpriseId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<InventoryTransferResponse> result = transferService.list(enterpriseId, status,
                PageRequest.of(Math.max(page, 0), Math.max(1, Math.min(size, transferPageSize))));
        return ApiResponse.ok("Lấy danh sách phiếu điều chuyển thành công",
                PageResponse.of(result.getContent(), result.getNumber(), result.getSize(),
                        result.getTotalElements(), result.getTotalPages()));
    }

    /** Trả header và các dòng của phiếu; endpoint chỉ đọc nên không khóa tồn kho. */
    @GetMapping("/{transferId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('INVENTORY_READ_ALL')")
    public ApiResponse<InventoryTransferResponse> getById(@PathVariable String transferId) {
        InventoryTransferResponse response = transferService.getById(transferId);
        warehouseAuthorizationService.requireWarehouseAccess(response.sourceWarehouseId());
        warehouseAuthorizationService.requireWarehouseAccess(response.destinationWarehouseId());
        return ApiResponse.ok("Lấy chi tiết phiếu điều chuyển thành công",
                response);
    }
}
