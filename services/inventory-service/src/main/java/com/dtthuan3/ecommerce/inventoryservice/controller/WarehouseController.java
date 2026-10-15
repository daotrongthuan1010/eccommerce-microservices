package com.dtthuan3.ecommerce.inventoryservice.controller;

import com.dtthuan3.ecommerce.common.response.ApiResponse;
import com.dtthuan3.ecommerce.inventoryservice.dto.request.CreateWarehouseRequest;
import com.dtthuan3.ecommerce.inventoryservice.dto.request.SetActiveRequest;
import com.dtthuan3.ecommerce.inventoryservice.dto.request.UpdateWarehouseRequest;
import com.dtthuan3.ecommerce.inventoryservice.dto.response.WarehouseResponse;
import com.dtthuan3.ecommerce.inventoryservice.service.WarehouseManagementService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
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

    /** Tạo kho mới; chỉ ADMIN được tạo master data. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<WarehouseResponse> create(@Valid @RequestBody CreateWarehouseRequest request) {
        return ApiResponse.created(warehouseService.create(request));
    }

    /** Đọc một kho; USER và ADMIN được phép tra cứu. */
    @GetMapping("/{warehouseId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ApiResponse<WarehouseResponse> getById(
            @PathVariable @NotBlank @Size(max = 100) String warehouseId) {
        return ApiResponse.ok("Lấy thông tin Warehouse thành công", warehouseService.getById(warehouseId));
    }

    /** Liệt kê kho theo enterprise để client lấy ID dùng cho Location và phiếu nhập. */
    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ApiResponse<List<WarehouseResponse>> list(
            @RequestParam @NotBlank @Size(max = 100) String enterpriseId) {
        return ApiResponse.ok("Lấy danh sách Warehouse thành công",
                warehouseService.listByEnterprise(enterpriseId));
    }

    /** Sửa thông tin mô tả kho; không đổi enterprise sở hữu. */
    @PutMapping("/{warehouseId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<WarehouseResponse> update(
            @PathVariable @NotBlank @Size(max = 100) String warehouseId,
            @Valid @RequestBody UpdateWarehouseRequest request) {
        return ApiResponse.ok("Cập nhật Warehouse thành công",
                warehouseService.update(warehouseId, request));
    }

    /** Ngừng/kích hoạt kho mềm; không xóa kho để giữ tham chiếu lịch sử. */
    @PatchMapping("/{warehouseId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<WarehouseResponse> setStatus(
            @PathVariable @NotBlank @Size(max = 100) String warehouseId,
            @Valid @RequestBody SetActiveRequest request) {
        return ApiResponse.ok("Cập nhật trạng thái Warehouse thành công",
                warehouseService.setActive(warehouseId, request.active()));
    }
}
