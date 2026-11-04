package com.dtthuan3.ecommerce.orderservice.controller;

import com.dtthuan3.ecommerce.common.response.ApiResponse;
import com.dtthuan3.ecommerce.orderservice.dto.request.AddCartItemRequest;
import com.dtthuan3.ecommerce.orderservice.dto.request.UpdateCartItemRequest;
import com.dtthuan3.ecommerce.orderservice.dto.response.CartResponse;
import com.dtthuan3.ecommerce.orderservice.security.CurrentUser;
import com.dtthuan3.ecommerce.orderservice.security.SecurityUtils;
import com.dtthuan3.ecommerce.orderservice.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Gio hang cua toi: userId lay tu JWT (SecurityUtils) — khong tin userId tu body/query. */
@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public ApiResponse<CartResponse> getCart() {
        return ApiResponse.ok("Lấy giỏ hàng thành công",
                cartService.getCart(requireUserId()));
    }

    @PostMapping("/items")
    public ApiResponse<CartResponse> addItem(@Valid @RequestBody AddCartItemRequest request) {
        return ApiResponse.ok("Đã thêm vào giỏ hàng",
                cartService.addItem(requireUserId(), request));
    }

    @PutMapping("/items/{skuId}")
    public ApiResponse<CartResponse> updateItem(@PathVariable String skuId,
                                                @Valid @RequestBody UpdateCartItemRequest request) {
        return ApiResponse.ok("Đã cập nhật giỏ hàng",
                cartService.updateItem(requireUserId(), skuId, request));
    }

    @DeleteMapping("/items/{skuId}")
    public ApiResponse<CartResponse> removeItem(@PathVariable String skuId) {
        return ApiResponse.ok("Đã xóa khỏi giỏ hàng",
                cartService.removeItem(requireUserId(), skuId));
    }

    private static String requireUserId() {
        CurrentUser user = SecurityUtils.requireUser();
        return user.subject();
    }
}
