package com.dtthuan3.ecommerce.orderservice.controller;

import com.dtthuan3.ecommerce.common.response.ApiResponse;
import com.dtthuan3.ecommerce.common.response.PageResponse;
import com.dtthuan3.ecommerce.orderservice.dto.request.CheckoutRequest;
import com.dtthuan3.ecommerce.orderservice.dto.response.CheckoutResponse;
import com.dtthuan3.ecommerce.orderservice.dto.response.OrderResponse;
import com.dtthuan3.ecommerce.orderservice.security.CurrentUser;
import com.dtthuan3.ecommerce.orderservice.security.SecurityUtils;
import com.dtthuan3.ecommerce.orderservice.service.OrderService;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Don hang cua toi (ORD-UC01/UC03). Idempotency-Key header bat buoc:
 * khach bam dat nhieu lan chi tao mot don.
 */
@RestController
@RequestMapping("/me")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/checkout")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CheckoutResponse> checkout(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CheckoutRequest request) {
        return ApiResponse.created(
                orderService.checkout(requireUserId(), idempotencyKey, request));
    }

    @GetMapping("/orders")
    public ApiResponse<PageResponse<OrderResponse>> myOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<OrderResponse> result = orderService.myOrders(requireUserId(), page, size);
        return ApiResponse.ok("Lấy danh sách đơn hàng thành công",
                PageResponse.of(result.getContent(), result.getNumber(), result.getSize(),
                        result.getTotalElements(), result.getTotalPages()));
    }

    @GetMapping("/orders/{orderId}")
    public ApiResponse<OrderResponse> myOrder(@PathVariable String orderId) {
        return ApiResponse.ok("Lấy đơn hàng thành công",
                orderService.myOrder(requireUserId(), orderId));
    }

    @PostMapping("/orders/{orderId}/cancel")
    public ApiResponse<OrderResponse> cancel(@PathVariable String orderId,
                                             @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.get("reason") : null;
        return ApiResponse.ok("Đã hủy đơn hàng",
                orderService.cancel(requireUserId(), orderId, reason));
    }

    private static String requireUserId() {
        CurrentUser user = SecurityUtils.requireUser();
        return user.subject();
    }
}
