package com.dtthuan3.ecommerce.orderservice.service;

import com.dtthuan3.ecommerce.orderservice.dto.request.AddCartItemRequest;
import com.dtthuan3.ecommerce.orderservice.dto.request.UpdateCartItemRequest;
import com.dtthuan3.ecommerce.orderservice.dto.response.CartResponse;

/** Gio hang (ORD-UC04): khong giu ton; tong tien chi la hien thi, quote moi la gia chot. */
public interface CartService {

    CartResponse getCart(String userId);

    CartResponse addItem(String userId, AddCartItemRequest request);

    CartResponse updateItem(String userId, String skuId, UpdateCartItemRequest request);

    CartResponse removeItem(String userId, String skuId);

    void clearCart(String userId);
}
