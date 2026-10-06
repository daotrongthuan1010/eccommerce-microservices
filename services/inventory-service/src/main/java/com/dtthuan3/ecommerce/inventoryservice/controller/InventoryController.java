package com.dtthuan3.ecommerce.inventoryservice.controller;

import com.dtthuan3.ecommerce.inventoryservice.dto.request.AdjustInventoryRequest;
import com.dtthuan3.ecommerce.inventoryservice.dto.request.CreateInventoryRequest;
import com.dtthuan3.ecommerce.inventoryservice.dto.request.IssueInventoryRequest;
import com.dtthuan3.ecommerce.inventoryservice.dto.request.ReceiveInventoryRequest;
import com.dtthuan3.ecommerce.inventoryservice.dto.request.ReceiveStockRequest;
import com.dtthuan3.ecommerce.inventoryservice.dto.request.ReleaseInventoryRequest;
import com.dtthuan3.ecommerce.inventoryservice.dto.request.ReserveInventoryRequest;
import com.dtthuan3.ecommerce.inventoryservice.dto.request.TransferInventoryRequest;
import com.dtthuan3.ecommerce.inventoryservice.dto.response.InventoryResponse;
import com.dtthuan3.ecommerce.inventoryservice.dto.response.StockMovementResponse;
import com.dtthuan3.ecommerce.inventoryservice.dto.response.TransferInventoryResponse;
import com.dtthuan3.ecommerce.inventoryservice.entity.MovementType;
import com.dtthuan3.ecommerce.inventoryservice.service.InventoryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Nhận HTTP request, kiểm tra DTO và chuyển xử lý nghiệp vụ cho InventoryService.
 * Mọi endpoint cần JWT hợp lệ; các thao tác thay đổi tồn chỉ dành cho ADMIN.
 */
@RestController
@RequestMapping("/inventory")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class InventoryController {

    private final InventoryService inventoryService;

    /** Lấy một inventory theo UUID/string ID. */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<InventoryResponse> getById(@PathVariable String id){
        return ResponseEntity.ok(inventoryService.getById(id));
    }


    /** Tìm chính xác một vị trí tồn theo enterprise, warehouse, location và SKU. */
    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public InventoryResponse getByPosition(
            @RequestParam String enterpriseId,
            @RequestParam String warehouseId,
            @RequestParam String locationId,
            @RequestParam String sku
    ) {

        return inventoryService.getByPosition(
                enterpriseId,
                warehouseId,
                locationId,
                sku
        );
    }

    /** Trả về tồn của SKU trên mọi vị trí/kho. */
    @GetMapping("/sku/{sku}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public List<InventoryResponse> bySku(@PathVariable String sku) {
        return inventoryService.bySku(sku);
    }

    /** Liệt kê các SKU đang được quản lý trong một warehouse. */
    @GetMapping("/warehouse/{warehouseId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public List<InventoryResponse> byWarehouse(@PathVariable String warehouseId) {
        return inventoryService.byWarehouse(warehouseId);
    }

    /** Liệt kê các SKU tại một location. */
    @GetMapping("/location/{locationId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public List<InventoryResponse> byLocation(@PathVariable String locationId) {
        return inventoryService.byLocation(locationId);
    }

    /** Tìm các inventory còn hàng khả dụng; có thể thu hẹp theo SKU/kho/vị trí. */
    @GetMapping("/available")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public List<InventoryResponse> available(@RequestParam(required = false) String sku,
                                             @RequestParam(required = false) String warehouseId,
                                             @RequestParam(required = false) String locationId) {
        return inventoryService.available(sku, warehouseId, locationId);
    }

    /** Tra lịch sử có lọc và phân trang; giới hạn size để tránh truy vấn quá lớn. */
    @GetMapping("/movements")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public Page<StockMovementResponse> movements(
            @RequestParam(required = false) String inventoryId,
            @RequestParam(required = false) MovementType type,
            @RequestParam(required = false) String referenceId,
            @RequestParam(required = false) LocalDateTime from,
            @RequestParam(required = false) LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return inventoryService.movementHistory(inventoryId, type, referenceId, from, to,
                PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, 200))));
    }

    /** Tạo vị trí tồn mới với các số lượng ban đầu bằng 0. */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public InventoryResponse create(
            @Valid @RequestBody CreateInventoryRequest request
    ) {
        return inventoryService.create(request);
    }

    /** Tạo một lô inventory cùng transaction; giới hạn kích thước để tránh batch quá lớn. */
    @PostMapping("/batch")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public List<InventoryResponse> createBatch(
            @Valid @RequestBody @NotEmpty List<@Valid CreateInventoryRequest> requests
    ) {
        return inventoryService.createBatch(requests);
    }

    /** Nhập hàng theo bộ khóa enterprise/warehouse/location/SKU; nếu chưa có thì tạo inventory. */
    @PostMapping("/receive-stock")
    @PreAuthorize("hasRole('ADMIN')")
    public InventoryResponse receiveStock(@Valid @RequestBody ReceiveStockRequest request,
                                          Authentication authentication) {
        return inventoryService.receiveStock(request,
                authentication == null ? null : authentication.getName());
    }

    /** Cộng hàng vào một inventory đã có. */
    @PostMapping("/receive")
    @PreAuthorize("hasRole('ADMIN')")
    public InventoryResponse receive(
            @Valid @RequestBody ReceiveInventoryRequest request
    ) {
        return inventoryService.receive(request);
    }

    /** Giữ trước một lượng hàng khả dụng. */
    @PostMapping("/reserve")
    @PreAuthorize("hasRole('ADMIN')")
    public InventoryResponse reserve(
            @Valid @RequestBody ReserveInventoryRequest request
    ) {
        return inventoryService.reserve(request);
    }
    /** Bỏ giữ hàng đã được giữ trước đó. */
    @PostMapping("/release")
    @PreAuthorize("hasRole('ADMIN')")
    public InventoryResponse release(
            @Valid @RequestBody ReleaseInventoryRequest request
    ) {
        return inventoryService.release(request);
    }

    /** Xuất hàng nhưng không cho lấy phần đã giữ/lỗi/đang chờ. */
    @PostMapping("/issue")
    @PreAuthorize("hasRole('ADMIN')")
    public InventoryResponse issue(
            @Valid @RequestBody IssueInventoryRequest request
    ) {
        return inventoryService.issue(request);
    }

    /** Điều chỉnh tồn vật lý theo kết quả kiểm kê thực tế. */
    @PostMapping("/adjust")
    @PreAuthorize("hasRole('ADMIN')")
    public InventoryResponse adjust(
            @Valid @RequestBody AdjustInventoryRequest request
    ) {
        return inventoryService.adjust(request);
    }

    /** Chuyển hàng giữa hai vị trí tồn; service cập nhật hai đầu và ghi hai biến động nguyên tử. */
    @PostMapping("/transfer")
    @PreAuthorize("hasRole('ADMIN')")
    public TransferInventoryResponse transfer(@Valid @RequestBody TransferInventoryRequest request) {
        return inventoryService.transfer(request);
    }
}
