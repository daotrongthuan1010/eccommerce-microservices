package com.dtthuan3.ecommerce.inventoryservice.controller;

import com.dtthuan3.ecommerce.common.response.ApiResponse;
import com.dtthuan3.ecommerce.common.response.PageResponse;
import com.dtthuan3.ecommerce.inventoryservice.dto.request.OpenInventoryCountSessionRequest;
import com.dtthuan3.ecommerce.inventoryservice.dto.request.RecordInventoryCountRequest;
import com.dtthuan3.ecommerce.inventoryservice.dto.request.RequestInventoryCountRecountRequest;
import com.dtthuan3.ecommerce.inventoryservice.dto.response.InventoryCountSessionResponse;
import com.dtthuan3.ecommerce.inventoryservice.dto.response.InventoryCountSessionLineResponse;
import com.dtthuan3.ecommerce.inventoryservice.dto.response.InventoryCountSubmissionResponse;
import com.dtthuan3.ecommerce.inventoryservice.dto.response.InventoryCountSessionSubmitResponse;
import com.dtthuan3.ecommerce.inventoryservice.dto.response.InventoryCountSessionApprovalResponse;
import com.dtthuan3.ecommerce.inventoryservice.dto.response.InventoryCountRecountResponse;
import com.dtthuan3.ecommerce.inventoryservice.service.InventoryCountSessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Điểm vào HTTP cho use case mở phiên kiểm kê; controller chỉ kiểm tra request và chuyển quyền cho service. */
@RestController
@RequestMapping("/inventory/count-sessions")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class InventoryCountSessionController {
    private final InventoryCountSessionService countSessionService;

    /** Liệt kê phiên kiểm kê trong enterprise, có thể lọc kho/trạng thái và phân trang. */
    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ApiResponse<PageResponse<InventoryCountSessionResponse>> list(
            @RequestParam String enterpriseId,
            @RequestParam(required = false) String warehouseId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Page<InventoryCountSessionResponse> result = countSessionService.list(enterpriseId, warehouseId,
                status, PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, 200))));
        return ApiResponse.ok("Lấy danh sách phiên kiểm kê thành công",
                PageResponse.of(result.getContent(), result.getNumber(), result.getSize(),
                        result.getTotalElements(), result.getTotalPages()));
    }

    /** Mở phiên kiểm kê và chụp snapshot baseline; endpoint chỉ dành cho người dùng ADMIN. */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<InventoryCountSessionResponse> open(
            @Valid @RequestBody OpenInventoryCountSessionRequest request,
            Authentication authentication) {
        String openedBy = authentication == null ? null : authentication.getName();
        return new ApiResponse<>(true, "CREATED", "Đã mở phiên kiểm kê và chụp baseline",
                countSessionService.open(request, openedBy));
    }

    /** Đọc baseline có phân trang để thiết bị kiểm kê xử lý từng lô nhỏ cho kho nhiều SKU. */
    @GetMapping("/{sessionId}/lines")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ApiResponse<PageResponse<InventoryCountSessionLineResponse>> getLines(
            @PathVariable String sessionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Page<InventoryCountSessionLineResponse> result = countSessionService.getLines(sessionId, page, size);
        return ApiResponse.ok("Lấy baseline phiên kiểm kê thành công",
                PageResponse.of(result.getContent(), result.getNumber(), result.getSize(),
                        result.getTotalElements(), result.getTotalPages()));
    }

    /** Ghi nhận một lô đếm vật lý; observation được lưu dạng audit và không sửa trực tiếp balance. */
    @PostMapping("/{sessionId}/counts")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<InventoryCountSubmissionResponse> recordCounts(
            @PathVariable String sessionId,
            @Valid @RequestBody RecordInventoryCountRequest request,
            Authentication authentication) {
        String recordedBy = authentication == null ? null : authentication.getName();
        return ApiResponse.ok("Đã ghi nhận lô kiểm đếm",
                countSessionService.recordCounts(sessionId, request, recordedBy));
    }

    /** Gửi phiên đã đếm đầy đủ sang hàng đợi duyệt; việc điều chỉnh tồn cần API duyệt riêng. */
    @PostMapping("/{sessionId}/submit")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<InventoryCountSessionSubmitResponse> submit(@PathVariable String sessionId) {
        return ApiResponse.ok("Đã gửi phiên kiểm kê sang trạng thái chờ duyệt",
                countSessionService.submit(sessionId));
    }

    /** Duyệt số đếm và ghi điều chỉnh tồn cùng lịch sử/outbox trong một transaction. */
    @PostMapping("/{sessionId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<InventoryCountSessionApprovalResponse> approve(
            @PathVariable String sessionId, Authentication authentication) {
        String approvedBy = authentication == null ? null : authentication.getName();
        return ApiResponse.ok("Đã duyệt phiên kiểm kê và áp dụng chênh lệch tồn",
                countSessionService.approve(sessionId, approvedBy));
    }

    /** Trả phiên về OPEN khi cần xác minh lại; request lưu vào audit thay vì xóa số đếm trước đó. */
    @PostMapping("/{sessionId}/recount")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<InventoryCountRecountResponse> requestRecount(
            @PathVariable String sessionId,
            @Valid @RequestBody RequestInventoryCountRecountRequest request,
            Authentication authentication) {
        String requestedBy = authentication == null ? null : authentication.getName();
        return ApiResponse.ok("Đã yêu cầu kiểm đếm lại phiên",
                countSessionService.requestRecount(sessionId, request, requestedBy));
    }
}
