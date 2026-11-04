package com.dtthuan3.ecommerce.orderservice.service.impl;

import com.dtthuan3.ecommerce.common.exception.BusinessException;
import com.dtthuan3.ecommerce.common.exception.ErrorCode;
import com.dtthuan3.ecommerce.common.exception.ResourceNotFoundException;
import com.dtthuan3.ecommerce.orderservice.dto.request.AddCartItemRequest;
import com.dtthuan3.ecommerce.orderservice.dto.request.UpdateCartItemRequest;
import com.dtthuan3.ecommerce.orderservice.dto.response.CartResponse;
import com.dtthuan3.ecommerce.orderservice.entity.Cart;
import com.dtthuan3.ecommerce.orderservice.entity.CartItem;
import com.dtthuan3.ecommerce.orderservice.repository.CartItemRepository;
import com.dtthuan3.ecommerce.orderservice.repository.CartRepository;
import com.dtthuan3.ecommerce.orderservice.service.CartService;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;

    @Override
    @Transactional
    public CartResponse getCart(String userId) {
        Cart cart = findOrCreateCart(userId);
        return toResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse addItem(String userId, AddCartItemRequest request) {
        Cart cart = findOrCreateCart(userId);

        CartItem item = cartItemRepository
                .findByCartIdAndSkuId(cart.getId(), request.skuId())
                .orElseGet(() -> {
                    CartItem fresh = new CartItem();
                    fresh.setId(UUID.randomUUID().toString());
                    fresh.setCartId(cart.getId());
                    fresh.setSkuId(request.skuId());
                    fresh.setProductName(request.productName());
                    fresh.setUnitPrice(request.unitPrice());
                    fresh.setQuantity(0);
                    return fresh;
                });

        item.setProductName(request.productName());
        item.setUnitPrice(request.unitPrice());
        item.setQuantity(item.getQuantity() + request.quantity());

        cartItemRepository.save(item);
        return toResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse updateItem(String userId, String skuId, UpdateCartItemRequest request) {
        Cart cart = findOrCreateCart(userId);

        CartItem item = cartItemRepository
                .findByCartIdAndSkuId(cart.getId(), skuId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item", skuId));

        if (request.quantity() == 0) {
            cartItemRepository.delete(item);
        } else {
            item.setQuantity(request.quantity());
            cartItemRepository.save(item);
        }
        return toResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse removeItem(String userId, String skuId) {
        Cart cart = findOrCreateCart(userId);

        CartItem item = cartItemRepository
                .findByCartIdAndSkuId(cart.getId(), skuId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item", skuId));

        cartItemRepository.delete(item);
        return toResponse(cart);
    }

    @Override
    @Transactional
    public void clearCart(String userId) {
        Cart cart = findOrCreateCart(userId);
        cartItemRepository.deleteByCartId(cart.getId());
    }

    private Cart findOrCreateCart(String userId) {
        return cartRepository
                .findByUserIdAndStatus(userId, "ACTIVE")
                .orElseGet(() -> {
                    Cart fresh = new Cart();
                    fresh.setId(UUID.randomUUID().toString());
                    fresh.setUserId(userId);
                    fresh.setStatus("ACTIVE");
                    return cartRepository.save(fresh);
                });
    }

    private CartResponse toResponse(Cart cart) {
        List<CartItem> items = cartItemRepository.findByCartIdOrderByCreatedAtAsc(cart.getId());

        List<CartResponse.Item> lines = new ArrayList<>();
        int totalQuantity = 0;
        long totalAmount = 0;

        for (CartItem item : items) {
            lines.add(new CartResponse.Item(
                    item.getSkuId(), item.getProductName(),
                    item.getUnitPrice(), item.getQuantity(), item.lineAmount()));
            totalQuantity += item.getQuantity();
            totalAmount += item.lineAmount();
        }

        return new CartResponse(cart.getId(), cart.getUserId(),
                lines, totalQuantity, totalAmount, cart.getUpdatedAt());
    }
}
