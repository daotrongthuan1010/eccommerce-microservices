package com.dtthuan3.ecommerce.orderservice.controller;

import com.dtthuan3.ecommerce.common.response.ApiResponse;
import com.dtthuan3.ecommerce.common.response.PageResponse;
import com.dtthuan3.ecommerce.orderservice.dto.request.AdminOrderStatusRequest;
import com.dtthuan3.ecommerce.orderservice.dto.response.OrderResponse;
import com.dtthuan3.ecommerce.orderservice.service.AdminOrderService;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Quan tri don hang (ADMIN): xem toan bo don, chuyen trang thai fulfill,
 * huy don bat ky trang thai non-terminal. Khac /me/*: khong scope theo userId.
 */
@RestController
@RequestMapping("/admin/orders")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminOrderController {

    private final AdminOrderService adminOrderService;

    @GetMapping
    public ApiResponse<PageResponse<OrderResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search) {
        Page<OrderResponse> result = adminOrderService.list(page, size, status, search);
        return ApiResponse.ok("Lấy danh sách đơn hàng thành công",
                PageResponse.of(result.getContent(), result.getNumber(), result.getSize(),
                        result.getTotalElements(), result.getTotalPages()));
    }

    @GetMapping("/{orderId}")
    public ApiResponse<OrderResponse> detail(@PathVariable String orderId) {
        return ApiResponse.ok("Lấy đơn hàng thành công", adminOrderService.detail(orderId));
    }

    @PostMapping("/{orderId}/status")
    public ApiResponse<OrderResponse> updateStatus(
            @PathVariable String orderId,
            @Valid @RequestBody AdminOrderStatusRequest request) {
        return ApiResponse.ok("Đã cập nhật trạng thái đơn hàng",
                adminOrderService.updateStatus(orderId, request.status()));
    }

    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<ApiResponse<OrderResponse>> cancel(
            @PathVariable String orderId,
            @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.get("reason") : null;
        return ResponseEntity.status(200).body(ApiResponse.ok("Đã hủy đơn hàng",
                adminOrderService.cancel(orderId, reason)));
    }
}
