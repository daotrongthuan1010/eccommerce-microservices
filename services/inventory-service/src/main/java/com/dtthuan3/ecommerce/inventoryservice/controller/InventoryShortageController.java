package com.dtthuan3.ecommerce.inventoryservice.controller;

import com.dtthuan3.ecommerce.common.response.ApiResponse;
import com.dtthuan3.ecommerce.inventoryservice.dto.request.ReportInventoryShortageRequest;
import com.dtthuan3.ecommerce.inventoryservice.dto.response.InventoryShortageCaseResponse;
import com.dtthuan3.ecommerce.inventoryservice.dto.response.InventoryShortageCaseDetailResponse;
import com.dtthuan3.ecommerce.inventoryservice.service.InventoryShortageService;
import com.dtthuan3.ecommerce.inventoryservice.service.InventoryWarehouseAuthorizationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.beans.factory.annotation.Value;
import com.dtthuan3.ecommerce.common.response.PageResponse;

/**
 * Endpoint tiếp nhận thiếu hàng phát hiện tại bước soạn; không tự đổi reservation hay tồn kho.
 * Class này là ranh giới HTTP của Inventory: nhận path/query/body, áp dụng xác thực và kiểm tra đầu vào,
 * gọi service nghiệp vụ rồi đóng gói HTTP response; không tự thực hiện truy vấn SQL hay cập nhật tồn.
 */
@RestController
@RequestMapping("/inventory/shortages")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class InventoryShortageController {

    @Value("${inventory.api.max-page-size}")
    private int maxPageSize;
    private final InventoryShortageService shortageService;
    private final InventoryWarehouseAuthorizationService warehouseAuthorizationService;

    /** Liệt kê shortage theo doanh nghiệp/trạng thái; giới hạn trang để tránh tải toàn bộ lịch sử. */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('INVENTORY_READ_ALL')")
    public ApiResponse<PageResponse<InventoryShortageCaseResponse>> list(
            @RequestParam String enterpriseId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Page<InventoryShortageCaseResponse> result = shortageService.list(enterpriseId, status,
                PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, maxPageSize))));
        return ApiResponse.ok("Lấy danh sách shortage case thành công",
                PageResponse.of(result.getContent(), result.getNumber(), result.getSize(),
                        result.getTotalElements(), result.getTotalPages()));
    }

    /** Đọc hồ sơ thiếu cùng các lần reserve thay thế và lượng thiếu còn lại. */
    @GetMapping("/{shortageCaseId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('INVENTORY_READ_ALL')")
    public ApiResponse<InventoryShortageCaseDetailResponse> getById(
            @PathVariable String shortageCaseId) {
        InventoryShortageCaseDetailResponse result = shortageService.getById(shortageCaseId);
        warehouseAuthorizationService.requireWarehouseAccess(result.shortageCase().warehouseId());
        return ApiResponse.ok("Lấy chi tiết shortage case thành công",
                result);
    }

    /** Tạo hồ sơ thiếu hàng; request trùng operationId và payload sẽ nhận lại cùng case. */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('INVENTORY_FULFILLMENT')")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<InventoryShortageCaseResponse> report(
            @Valid @RequestBody ReportInventoryShortageRequest request,
            Authentication authentication) {
        String reportedBy = authentication == null ? null : authentication.getName();
        return new ApiResponse<>(true, "CREATED", "Đã ghi nhận hồ sơ thiếu hàng",
                shortageService.report(request, reportedBy));
    }
}
