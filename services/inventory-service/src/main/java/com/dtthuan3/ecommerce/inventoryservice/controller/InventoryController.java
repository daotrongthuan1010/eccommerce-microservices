package com.dtthuan3.ecommerce.inventoryservice.controller;

import com.dtthuan3.ecommerce.common.response.ApiResponse;
import com.dtthuan3.ecommerce.common.response.PageResponse;
import com.dtthuan3.ecommerce.inventoryservice.client.ProductSkuClient;
import com.dtthuan3.ecommerce.inventoryservice.dto.request.AdjustInventoryRequest;
import com.dtthuan3.ecommerce.inventoryservice.dto.request.CreateInventoryTransferRequest;
import com.dtthuan3.ecommerce.inventoryservice.dto.request.CreateInventoryRequest;
import com.dtthuan3.ecommerce.inventoryservice.dto.request.IssueInventoryRequest;
import com.dtthuan3.ecommerce.inventoryservice.dto.request.ReceiveInventoryRequest;
import com.dtthuan3.ecommerce.inventoryservice.dto.request.ReceiveStockRequest;
import com.dtthuan3.ecommerce.inventoryservice.dto.request.ReceiveReturnReceiptRequest;
import com.dtthuan3.ecommerce.inventoryservice.dto.request.InspectReturnReceiptRequest;
import com.dtthuan3.ecommerce.inventoryservice.dto.request.ReleaseInventoryRequest;
import com.dtthuan3.ecommerce.inventoryservice.dto.request.ReserveInventoryRequest;
import com.dtthuan3.ecommerce.inventoryservice.dto.request.TransferInventoryRequest;
import com.dtthuan3.ecommerce.inventoryservice.dto.response.InventoryResponse;
import com.dtthuan3.ecommerce.inventoryservice.dto.response.InventoryTransferResponse;
import com.dtthuan3.ecommerce.inventoryservice.dto.response.ReturnReceiptResponse;
import com.dtthuan3.ecommerce.inventoryservice.dto.response.ReturnInspectionResponse;
import com.dtthuan3.ecommerce.inventoryservice.dto.response.StockMovementResponse;
import com.dtthuan3.ecommerce.inventoryservice.dto.response.TransferInventoryResponse;
import com.dtthuan3.ecommerce.inventoryservice.entity.MovementType;
import com.dtthuan3.ecommerce.inventoryservice.service.InventoryService;
import com.dtthuan3.ecommerce.inventoryservice.service.InventoryTransferService;
import com.dtthuan3.ecommerce.inventoryservice.service.InventoryReturnReceiptService;
import com.dtthuan3.ecommerce.inventoryservice.service.InventoryReturnInspectionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Nhận HTTP request, kiểm tra DTO và chuyển xử lý nghiệp vụ cho InventoryService.
 * Mọi endpoint cần JWT hợp lệ; các endpoint ghi hiện chỉ cho phép ADMIN.
 */
@RestController
@RequestMapping("/inventory")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class InventoryController {

    private final InventoryService inventoryService;
    private final InventoryReturnReceiptService returnReceiptService;
    private final InventoryReturnInspectionService returnInspectionService;
    private final InventoryTransferService transferService;
    private final ProductSkuClient productSkuClient;

    /** Tra cứu một Inventory theo UUID hoặc ID dạng chuỗi. */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ApiResponse<InventoryResponse> getById(@PathVariable String id){
        return ApiResponse.ok("Lấy thông tin Inventory thành công", inventoryService.getById(id));
    }


    /** Tra cứu chính xác một vị trí tồn theo enterpriseId, warehouseId, locationId và SKU. */
    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ApiResponse<InventoryResponse> getByPosition(
            @RequestParam String enterpriseId,
            @RequestParam String warehouseId,
            @RequestParam String locationId,
            @RequestParam String sku
    ) {

        return ApiResponse.ok("Tra cứu Inventory theo vị trí thành công", inventoryService.getByPosition(
                enterpriseId,
                warehouseId,
                locationId,
                sku
        ));
    }

    /** Tra cứu tồn của SKU trên mọi Location và Warehouse theo trang. */
    @GetMapping("/sku/{sku}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ApiResponse<PageResponse<InventoryResponse>> bySku(
            @PathVariable String sku,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return toPageResponse(inventoryService.bySku(sku, pageRequest(page, size)),
                "Tra cứu tồn theo SKU thành công");
    }

    /** Liệt kê tồn trong một Warehouse theo trang. */
    @GetMapping("/warehouse/{warehouseId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ApiResponse<PageResponse<InventoryResponse>> byWarehouse(
            @PathVariable String warehouseId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return toPageResponse(inventoryService.byWarehouse(warehouseId, pageRequest(page, size)),
                "Tra cứu tồn theo Warehouse thành công");
    }

    /** Liệt kê tồn tại một Location theo trang. */
    @GetMapping("/location/{locationId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ApiResponse<PageResponse<InventoryResponse>> byLocation(
            @PathVariable String locationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return toPageResponse(inventoryService.byLocation(locationId, pageRequest(page, size)),
                "Tra cứu tồn theo Location thành công");
    }

    /** Tra cứu Inventory còn hàng khả dụng theo trang; có thể lọc thêm theo SKU, Warehouse hoặc Location. */
    @GetMapping("/available")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ApiResponse<PageResponse<InventoryResponse>> available(@RequestParam(required = false) String sku,
                                                                  @RequestParam(required = false) String warehouseId,
                                                                  @RequestParam(required = false) String locationId,
                                                                  @RequestParam(defaultValue = "0") int page,
                                                                  @RequestParam(defaultValue = "50") int size) {
        return toPageResponse(inventoryService.available(sku, warehouseId, locationId, pageRequest(page, size)),
                "Tra cứu tồn khả dụng thành công");
    }

    /** Tra cứu lịch sử có bộ lọc và phân trang; giới hạn size để tránh truy vấn quá lớn. */
    @GetMapping("/movements")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ApiResponse<PageResponse<StockMovementResponse>> movements(
            @RequestParam(required = false) String inventoryId,
            @RequestParam(required = false) MovementType type,
            @RequestParam(required = false) String referenceId,
            @RequestParam(required = false) LocalDateTime from,
            @RequestParam(required = false) LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Page<StockMovementResponse> result = inventoryService.movementHistory(
                inventoryId, type, referenceId, from, to,
                PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, 200))));
        return toPageResponse(result, "Lấy lịch sử biến động tồn kho thành công");
    }

    /** Tạo Inventory mới với các số lượng ban đầu bằng 0. */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<InventoryResponse> create(
            @Valid @RequestBody CreateInventoryRequest request
    ) {
        productSkuClient.requireActiveSkus(List.of(request.sku()));
        return created("Tạo Inventory thành công", inventoryService.create(request));
    }

    /** Tạo một Batch Inventory trong cùng transaction; giới hạn kích thước để tránh Batch quá lớn. */
    @PostMapping("/batch")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<List<InventoryResponse>> createBatch(
            @Valid @RequestBody @NotEmpty(message = "Danh sách Inventory không được để trống")
            List<@NotNull(message = "Phần tử Inventory không được để trống") @Valid CreateInventoryRequest> requests
    ) {
        productSkuClient.requireActiveSkus(requests.stream().map(CreateInventoryRequest::sku).distinct().toList());
        return created("Tạo Batch Inventory thành công", inventoryService.createBatch(requests));
    }

    /** Nhập hàng theo enterpriseId, warehouseId, locationId và SKU; tạo Inventory nếu chưa tồn tại. */
    @PostMapping("/receive-stock")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<InventoryResponse> receiveStock(@Valid @RequestBody ReceiveStockRequest request,
                                                        Authentication authentication) {
        productSkuClient.requireActiveSkus(List.of(request.sku()));
        return ApiResponse.ok("Ghi nhận nhập hàng thành công", inventoryService.receiveStock(request,
                authentication == null ? null : authentication.getName()));
    }

    /** Nhận parcel trả hàng đã duyệt; số lượng vào pending để chờ kiểm tra chất lượng. */
    @PostMapping("/returns/receive")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ReturnReceiptResponse> receiveReturn(
            @Valid @RequestBody ReceiveReturnReceiptRequest request,
            Authentication authentication) {
        return created("Đã nhận hàng trả vào khu cách ly",
                returnReceiptService.receive(request, authentication == null ? null : authentication.getName()));
    }

    /** Tra cứu biên nhận parcel trả hàng và biết parcel còn cách ly hay đã kiểm tra. */
    @GetMapping("/returns/receipts/{receiptId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ReturnReceiptResponse> getReturnReceipt(@PathVariable String receiptId) {
        return ApiResponse.ok("Lấy thông tin biên nhận hàng trả thành công",
                returnReceiptService.getById(receiptId));
    }

    /** Khôi phục kết quả phân loại đã chốt nếu client mất response của lần kiểm định trước. */
    @GetMapping("/returns/receipts/{receiptId}/inspection")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ReturnInspectionResponse> getReturnInspection(@PathVariable String receiptId) {
        return ApiResponse.ok("Lấy kết quả kiểm tra hàng trả thành công",
                returnInspectionService.getByReceiptId(receiptId));
    }

    /** Kiểm tra toàn bộ parcel đã nhận và chuyển số lượng ra khỏi khu chờ kiểm tra. */
    @PostMapping("/returns/{receiptId}/inspect")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ReturnInspectionResponse> inspectReturn(
            @PathVariable String receiptId,
            @Valid @RequestBody InspectReturnReceiptRequest request,
            Authentication authentication) {
        return created("Đã kiểm tra và phân loại hàng trả",
                returnInspectionService.inspect(receiptId, request,
                        authentication == null ? null : authentication.getName()));
    }

    /** Cộng hàng vào một Inventory đã có. */
    @PostMapping("/receive")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<InventoryResponse> receive(
            @Valid @RequestBody ReceiveInventoryRequest request
    ) {
        InventoryResponse current = inventoryService.getById(request.inventoryId());
        productSkuClient.requireActiveSkus(List.of(current.sku()));
        return ApiResponse.ok("Nhập thêm hàng vào Inventory thành công", inventoryService.receive(request));
    }

    /** Giữ trước một lượng hàng khả dụng trong Inventory. */
    @PostMapping("/reserve")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<InventoryResponse> reserve(
            @Valid @RequestBody ReserveInventoryRequest request
    ) {
        return ApiResponse.ok("Giữ hàng thành công", inventoryService.reserve(request));
    }
    /** Bỏ giữ lượng hàng đã được Reserve trước đó. */
    @PostMapping("/release")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<InventoryResponse> release(
            @Valid @RequestBody ReleaseInventoryRequest request
    ) {
        return ApiResponse.ok("Bỏ giữ hàng thành công", inventoryService.release(request));
    }

    /** Xuất hàng nhưng không cho lấy phần đã Reserve, lỗi hoặc đang chờ. */
    @PostMapping("/issue")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<InventoryResponse> issue(
            @Valid @RequestBody IssueInventoryRequest request
    ) {
        return ApiResponse.ok("Xuất hàng thành công", inventoryService.issue(request));
    }

    /** Điều chỉnh tồn vật lý theo kết quả kiểm kê thực tế. */
    @PostMapping("/adjust")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<InventoryResponse> adjust(
            @Valid @RequestBody AdjustInventoryRequest request
    ) {
        return ApiResponse.ok("Điều chỉnh tồn kho thành công", inventoryService.adjust(request));
    }

    /** Chuyển hàng giữa hai vị trí tồn; service cập nhật cả hai đầu và ghi hai biến động nguyên tử. */
    @PostMapping("/transfer")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<TransferInventoryResponse> transfer(@Valid @RequestBody TransferInventoryRequest request) {
        return ApiResponse.ok("Chuyển tồn kho thành công", inventoryService.transfer(request));
    }

    /** Tạo phiếu điều chuyển nháp; tồn kho chỉ thay đổi ở API xuất chuyển sau này. */
    @PostMapping("/transfers")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<InventoryTransferResponse> createTransfer(
            @Valid @RequestBody CreateInventoryTransferRequest request,
            Authentication authentication) {
        return created("Đã tạo phiếu điều chuyển ở trạng thái CREATED; tồn kho chưa thay đổi",
                transferService.create(request, authentication == null ? null : authentication.getName()));
    }

    private PageRequest pageRequest(int page, int size) {
        return PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, 200)));
    }

    private <T> ApiResponse<PageResponse<T>> toPageResponse(Page<T> page, String message) {
        return ApiResponse.ok(message, PageResponse.of(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages()));
    }

    private <T> ApiResponse<T> created(String message, T data) {
        return new ApiResponse<>(true, "CREATED", message, data);
    }
}
