package com.dtthuan3.ecommerce.inventoryservice.controller;

import com.dtthuan3.ecommerce.common.response.ApiResponse;
import com.dtthuan3.ecommerce.inventoryservice.dto.request.GrantWarehouseAccessRequest;
import com.dtthuan3.ecommerce.inventoryservice.repository.InventoryWarehouseAccessGrantRepository.WarehouseAccessGrantRecord;
import com.dtthuan3.ecommerce.inventoryservice.service.InventoryWarehouseAuthorizationService;
import com.dtthuan3.ecommerce.inventoryservice.service.WarehouseManagementService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST API quản trị phạm vi truy cập theo kho; chỉ ADMIN mới được cấp, xem hoặc thu hồi grant.
 * Grant chỉ quyết định principal được chạm vào kho nào; mỗi API vẫn cần authority nghiệp vụ tương ứng.
 */
@RestController
@RequestMapping("/warehouses/{warehouseId}/access-grants")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasRole('ADMIN')")
public class InventoryWarehouseAccessController {
    private final InventoryWarehouseAuthorizationService authorizationService;
    private final WarehouseManagementService warehouseService;

    /** Cấp phạm vi cho subject JWT sau khi xác nhận warehouse tồn tại trong Inventory. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<WarehouseAccessGrantRecord> grant(
            @PathVariable @NotBlank @Size(max = 100) String warehouseId,
            @Valid @RequestBody GrantWarehouseAccessRequest request,
            Authentication authentication) {
        warehouseService.getById(warehouseId);
        return ApiResponse.created(authorizationService.grant(warehouseId, request.subjectId(),
                authentication.getName()));
    }

    /** Liệt kê grant hiện tại để kiểm soát quyền theo phạm vi kho. */
    @GetMapping
    public ApiResponse<List<WarehouseAccessGrantRecord>> list(
            @PathVariable @NotBlank @Size(max = 100) String warehouseId) {
        warehouseService.getById(warehouseId);
        return ApiResponse.ok("Lấy danh sách quyền theo Warehouse thành công",
                authorizationService.list(warehouseId));
    }

    /** Thu hồi grant của đúng một principal trên đúng một kho. */
    @DeleteMapping("/{subjectId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(@PathVariable @NotBlank @Size(max = 100) String warehouseId,
                       @PathVariable @NotBlank @Size(max = 255) String subjectId) {
        warehouseService.getById(warehouseId);
        authorizationService.revoke(warehouseId, subjectId);
    }
}
