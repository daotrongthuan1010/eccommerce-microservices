package com.dtthuan3.ecommerce.inventoryservice.controller;

import com.dtthuan3.ecommerce.common.response.ApiResponse;
import com.dtthuan3.ecommerce.common.response.PageResponse;
import com.dtthuan3.ecommerce.inventoryservice.dto.request.CreateLocationRequest;
import com.dtthuan3.ecommerce.inventoryservice.dto.request.SetActiveRequest;
import com.dtthuan3.ecommerce.inventoryservice.dto.request.UpdateLocationRequest;
import com.dtthuan3.ecommerce.inventoryservice.dto.response.LocationResponse;
import com.dtthuan3.ecommerce.inventoryservice.service.LocationManagementService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;


/** REST API quản lý Location thuộc một Warehouse nội bộ của Inventory. */
@RestController
@RequestMapping("/inventory/locations")
@RequiredArgsConstructor
@Validated
public class LocationController {

    private final LocationManagementService locationService;

    /** Tạo Location mới; mã Location phải duy nhất trong cùng một Warehouse. */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<LocationResponse> create(@Valid @RequestBody CreateLocationRequest request) {
        return new ApiResponse<>(true, "CREATED", "Tạo Location thành công", locationService.createLocation(request));
    }

    /** Đọc một Location theo ID; response luôn dùng cấu trúc ApiResponse chung của hệ thống. */
    @GetMapping("/{locationId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ApiResponse<LocationResponse> getById(@PathVariable String locationId) {
        return ApiResponse.ok("Lấy thông tin Location thành công", locationService.getLocation(locationId));
    }

    /** Liệt kê các Location thuộc warehouseId được truyền vào, đã sắp xếp theo code. */
    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ApiResponse<PageResponse<LocationResponse>> list(
            @RequestParam @NotBlank(message = "warehouseId là bắt buộc") String warehouseId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Page<LocationResponse> result = locationService.listLocations(warehouseId,
                PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, 100))));
        return ApiResponse.ok("Lấy danh sách Location thành công",
                PageResponse.of(result.getContent(), result.getNumber(), result.getSize(),
                        result.getTotalElements(), result.getTotalPages()));
    }

    /** Cập nhật code và name; API không cho chuyển Location sang Warehouse khác. */
    @PutMapping("/{locationId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<LocationResponse> update(@PathVariable String locationId,
                                                @Valid @RequestBody UpdateLocationRequest request) {
        return ApiResponse.ok("Cập nhật Location thành công", locationService.updateLocation(locationId, request));
    }

    /** Bật hoặc ngừng sử dụng Location; dùng trạng thái thay cho xóa để giữ lịch sử tham chiếu. */
    @PatchMapping("/{locationId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<LocationResponse> setActive(@PathVariable String locationId,
                                                   @Valid @RequestBody SetActiveRequest request) {
        return ApiResponse.ok("Cập nhật trạng thái Location thành công",
                locationService.setLocationActive(locationId, request.active()));
    }
}
