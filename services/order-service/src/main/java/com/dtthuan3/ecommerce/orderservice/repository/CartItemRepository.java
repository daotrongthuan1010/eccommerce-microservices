package com.dtthuan3.ecommerce.orderservice.repository;

import com.dtthuan3.ecommerce.orderservice.entity.CartItem;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartItemRepository extends JpaRepository<CartItem, String> {
    List<CartItem> findByCartIdOrderByCreatedAtAsc(String cartId);
    Optional<CartItem> findByCartIdAndSkuId(String cartId, String skuId);
    void deleteByCartId(String cartId);
}
