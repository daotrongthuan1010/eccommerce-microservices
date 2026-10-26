package com.dtthuan3.ecommerce.inventoryservice.controller;

import com.dtthuan3.ecommerce.common.response.ApiResponse;
import com.dtthuan3.ecommerce.inventoryservice.dto.request.CommitInventoryReservationRequest;
import com.dtthuan3.ecommerce.inventoryservice.dto.request.CreateInventoryReservationRequest;
import com.dtthuan3.ecommerce.inventoryservice.dto.request.DispatchInventoryReservationRequest;
import com.dtthuan3.ecommerce.inventoryservice.dto.request.ReleaseInventoryReservationRequest;
import com.dtthuan3.ecommerce.inventoryservice.dto.response.InventoryReservationResponse;
import com.dtthuan3.ecommerce.inventoryservice.service.InventoryReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** REST endpoints cho reservation; controller xác thực/validate rồi chuyển nghiệp vụ cho service. */
@RestController
@RequestMapping("/inventory/reservations")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class InventoryReservationController {
    private final InventoryReservationService reservationService;

    /** Tạo reservation atomically; request phải giữ nguyên reservationId và operationId khi retry. */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<InventoryReservationResponse> reserve(
            @Valid @RequestBody CreateInventoryReservationRequest request, Authentication authentication) {
        return new ApiResponse<>(true, "CREATED", "Đã giữ hàng cho reservation",
                reservationService.reserve(request, authentication == null ? null : authentication.getName()));
    }

    /** Đọc trạng thái và tiến độ từng dòng bằng ID ổn định để caller tra cứu sau timeout. */
    @GetMapping("/{reservationId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<InventoryReservationResponse> getById(@PathVariable String reservationId) {
        return ApiResponse.ok("Lấy reservation thành công", reservationService.getById(reservationId));
    }

    /** Kết thúc reservation HELD đã quá hạn và nhả toàn bộ lượng đang giữ về tồn khả dụng. */
    @PostMapping("/{reservationId}/expire")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<InventoryReservationResponse> expire(@PathVariable String reservationId,
                                                               Authentication authentication) {
        return ApiResponse.ok("Đã xử lý hết hạn reservation", reservationService.expire(reservationId,
                authentication == null ? null : authentication.getName()));
    }

    /** Chuyển HELD sang COMMITTED sau khi Order xác nhận payment reference đã được kiểm chứng. */
    @PostMapping("/{reservationId}/commit")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<InventoryReservationResponse> commit(
            @PathVariable String reservationId,
            @Valid @RequestBody CommitInventoryReservationRequest request, Authentication authentication) {
        return ApiResponse.ok("Đã commit reservation", reservationService.commit(reservationId, request,
                authentication == null ? null : authentication.getName()));
    }

    /** Nhả toàn bộ hoặc một phần lượng chưa dispatch; operationId giúp retry không nhả lặp. */
    @PostMapping("/{reservationId}/release")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<InventoryReservationResponse> release(
            @PathVariable String reservationId,
            @Valid @RequestBody ReleaseInventoryReservationRequest request, Authentication authentication) {
        return ApiResponse.ok("Đã release lượng reservation được yêu cầu", reservationService.release(
                reservationId, request, authentication == null ? null : authentication.getName()));
    }

    /** Dispatch số lượng theo reservation line; không thể xuất vượt lượng đã commit còn lại. */
    @PostMapping("/{reservationId}/dispatch")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<InventoryReservationResponse> dispatch(
            @PathVariable String reservationId,
            @Valid @RequestBody DispatchInventoryReservationRequest request, Authentication authentication) {
        return ApiResponse.ok("Đã dispatch hàng theo reservation", reservationService.dispatch(
                reservationId, request, authentication == null ? null : authentication.getName()));
    }
}
