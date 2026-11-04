package com.dtthuan3.ecommerce.inventoryservice.controller;

import com.dtthuan3.ecommerce.common.response.ApiResponse;
import com.dtthuan3.ecommerce.common.response.PageResponse;
import com.dtthuan3.ecommerce.inventoryservice.dto.request.CreateWarehouseRequest;
import com.dtthuan3.ecommerce.inventoryservice.dto.request.SetActiveRequest;
import com.dtthuan3.ecommerce.inventoryservice.dto.request.UpdateWarehouseRequest;
import com.dtthuan3.ecommerce.inventoryservice.dto.response.WarehouseResponse;
import com.dtthuan3.ecommerce.inventoryservice.dto.response.WarehouseCursorPageResponse;
import com.dtthuan3.ecommerce.inventoryservice.service.WarehouseManagementService;
import com.dtthuan3.ecommerce.inventoryservice.service.InventoryWarehouseAuthorizationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST adapter quản lý Warehouse nội bộ.
 * Controller giữ phần HTTP/authorization; kiểm tra nghiệp vụ và persistence nằm ở service/repository.
 */
@RestController
@RequestMapping("/warehouses")
@RequiredArgsConstructor
@Validated
public class WarehouseController {
    private final WarehouseManagementService warehouseService;
    private final InventoryWarehouseAuthorizationService warehouseAuthorizationService;

    /** Tạo kho mới; chỉ ADMIN được tạo master data. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN') or hasRole('INVENTORY_MANAGE')")
    public ApiResponse<WarehouseResponse> create(@Valid @RequestBody CreateWarehouseRequest request) {
        return ApiResponse.created(warehouseService.create(request));
    }

    /** Đọc một kho; USER và ADMIN được phép tra cứu. */
    @GetMapping("/{warehouseId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('INVENTORY_READ')")
    public ApiResponse<WarehouseResponse> getById(
            @PathVariable @NotBlank @Size(max = 100) String warehouseId) {
        warehouseAuthorizationService.requireWarehouseAccess(warehouseId);
        return ApiResponse.ok("Lấy thông tin Warehouse thành công", warehouseService.getById(warehouseId));
    }

    /** Liệt kê tối đa 100 kho cho màn hình nhỏ; enterprise nhiều kho cần dùng API page/cursor. */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('INVENTORY_READ_ALL')")
    public ApiResponse<List<WarehouseResponse>> list(
            @RequestParam @NotBlank @Size(max = 100) String enterpriseId) {
        return ApiResponse.ok("Lấy danh sách Warehouse thành công",
                warehouseService.listByEnterprise(enterpriseId));
    }

    /** Endpoint phân trang cho enterprise có nhiều kho; giới hạn 100 item mỗi trang. */
    @GetMapping("/page")
    @PreAuthorize("hasRole('ADMIN') or hasRole('INVENTORY_READ_ALL')")
    public ApiResponse<PageResponse<WarehouseResponse>> listPage(
            @RequestParam @NotBlank @Size(max = 100) String enterpriseId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int size) {
        return ApiResponse.ok("Lấy danh sách Warehouse theo trang thành công",
                warehouseService.listPageByEnterprise(enterpriseId, page, size));
    }

    /** Cursor endpoint tránh COUNT/OFFSET sâu; gửi lại cặp nextCode/nextId để lấy trang kế tiếp. */
    @GetMapping("/cursor")
    @PreAuthorize("hasRole('ADMIN') or hasRole('INVENTORY_READ_ALL')")
    public ApiResponse<WarehouseCursorPageResponse> listCursor(
            @RequestParam @NotBlank @Size(max = 100) String enterpriseId,
            @RequestParam(required = false) @Size(max = 64) String afterCode,
            @RequestParam(required = false) @Size(max = 100) String afterId,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int size) {
        return ApiResponse.ok("Lấy danh sách Warehouse theo cursor thành công",
                warehouseService.listCursorByEnterprise(enterpriseId, afterCode, afterId, size));
    }

    /** Sửa thông tin mô tả kho; không đổi enterprise sở hữu. */
    @PutMapping("/{warehouseId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('INVENTORY_MANAGE')")
    public ApiResponse<WarehouseResponse> update(
            @PathVariable @NotBlank @Size(max = 100) String warehouseId,
            @Valid @RequestBody UpdateWarehouseRequest request) {
        warehouseAuthorizationService.requireWarehouseAccess(warehouseId);
        return ApiResponse.ok("Cập nhật Warehouse thành công",
                warehouseService.update(warehouseId, request));
    }

    /** Ngừng/kích hoạt kho mềm; không xóa kho để giữ tham chiếu lịch sử. */
    @PatchMapping("/{warehouseId}/status")
    @PreAuthorize("hasRole('ADMIN') or hasRole('INVENTORY_MANAGE')")
    public ApiResponse<WarehouseResponse> setStatus(
            @PathVariable @NotBlank @Size(max = 100) String warehouseId,
            @Valid @RequestBody SetActiveRequest request) {
        warehouseAuthorizationService.requireWarehouseAccess(warehouseId);
        return ApiResponse.ok("Cập nhật trạng thái Warehouse thành công",
                warehouseService.setActive(warehouseId, request.active()));
    }
}
