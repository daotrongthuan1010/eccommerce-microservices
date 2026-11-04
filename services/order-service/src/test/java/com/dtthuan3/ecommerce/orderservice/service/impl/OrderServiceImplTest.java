package com.dtthuan3.ecommerce.orderservice.service.impl;

import com.dtthuan3.ecommerce.common.exception.BusinessException;
import com.dtthuan3.ecommerce.common.exception.ErrorCode;
import com.dtthuan3.ecommerce.common.exception.ResourceNotFoundException;
import com.dtthuan3.ecommerce.orderservice.client.InventoryClient;
import com.dtthuan3.ecommerce.orderservice.client.PaymentClient;
import com.dtthuan3.ecommerce.orderservice.dto.request.CheckoutRequest;
import com.dtthuan3.ecommerce.orderservice.dto.response.CheckoutResponse;
import com.dtthuan3.ecommerce.orderservice.dto.response.OrderResponse;
import com.dtthuan3.ecommerce.orderservice.entity.Cart;
import com.dtthuan3.ecommerce.orderservice.entity.CartItem;
import com.dtthuan3.ecommerce.orderservice.entity.Order;
import com.dtthuan3.ecommerce.orderservice.entity.OrderOutbox;
import com.dtthuan3.ecommerce.orderservice.repository.CartItemRepository;
import com.dtthuan3.ecommerce.orderservice.repository.CartRepository;
import com.dtthuan3.ecommerce.orderservice.repository.OrderLineRepository;
import com.dtthuan3.ecommerce.orderservice.repository.OrderOutboxRepository;
import com.dtthuan3.ecommerce.orderservice.repository.OrderRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * P1 checkpoint: "Double click checkout chi mot don" (ORD-UC03).
 * Idempotency theo (idempotencyKey, requestHash).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrderServiceImplTest {

    private static final String USER = "user-01";
    private static final String KEY = "idem-key-01";

    @Mock private CartRepository cartRepository;
    @Mock private CartItemRepository cartItemRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private OrderLineRepository orderLineRepository;
    @Mock private OrderOutboxRepository outboxRepository;
    @Mock private InventoryClient inventoryClient;
    @Mock private PaymentClient paymentClient;

    private OrderServiceImpl orderService;

    private final CheckoutRequest request = new CheckoutRequest(
            "123 Nguyen Hue, Q1", "Nguyen Van A", "0901234567", "COD");

    @BeforeEach
    void setUp() {
        orderService = new OrderServiceImpl(
                cartRepository, cartItemRepository, orderRepository,
                orderLineRepository, outboxRepository,
                inventoryClient, paymentClient, new ObjectMapper());
    }

    private Cart activeCart() {
        Cart cart = new Cart();
        cart.setId("cart-01");
        cart.setUserId(USER);
        return cart;
    }

    private CartItem cartItem(String skuId, long price, int qty) {
        CartItem item = new CartItem();
        item.setId("item-" + skuId);
        item.setCartId("cart-01");
        item.setSkuId(skuId);
        item.setProductName("San pham " + skuId);
        item.setUnitPrice(price);
        item.setQuantity(qty);
        return item;
    }

    @Test
    @DisplayName("Checkout thanh cong: tao don PENDING_PAYMENT + outbox OrderCreated")
    void checkout_createsPendingPaymentOrder_withOutboxEvent() {

        when(orderRepository.findByIdempotencyKey(KEY)).thenReturn(Optional.empty());
        when(cartRepository.findByUserIdAndStatus(USER, "ACTIVE"))
                .thenReturn(Optional.of(activeCart()));
        when(cartItemRepository.findByCartIdOrderByCreatedAtAsc("cart-01"))
                .thenReturn(List.of(cartItem("sku-1", 100_000, 2)));
        when(orderRepository.save(any(Order.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        CheckoutResponse response = orderService.checkout(USER, KEY, request);

        assertThat(response.status()).isEqualTo("PENDING_PAYMENT");
        assertThat(response.totalAmount()).isEqualTo(200_000L);
        assertThat(response.orderCode()).startsWith("ORD-");
        assertThat(response.lines()).hasSize(1);
        assertThat(response.lines().get(0).lineAmount()).isEqualTo(200_000L);

        // Outbox event ghi cung transaction
        ArgumentCaptor<OrderOutbox> outboxCaptor = ArgumentCaptor.forClass(OrderOutbox.class);
        verify(outboxRepository).save(outboxCaptor.capture());
        OrderOutbox outbox = outboxCaptor.getValue();
        assertThat(outbox.getEventType()).isEqualTo("OrderCreated");
        assertThat(outbox.getTopic()).isEqualTo("order-events");
        assertThat(outbox.getAggregateId()).isEqualTo(response.orderId());
    }

    @Test
    @DisplayName("ORD-UC03: bam dat nhieu lan cung key -> tra lai cung don, khong tao don thu 2")
    void checkout_sameKeySameRequest_returnsExistingOrder() {

        Order existing = new Order();
        existing.setId("order-01");
        existing.setOrderCode("ORD-241104-000001");
        existing.setUserId(USER);
        existing.setStatus("PENDING_PAYMENT");
        existing.setTotalAmount(200_000L);
        existing.setCurrency("VND");
        existing.setPaymentMethod("COD");
        existing.setShippingAddress(request.shippingAddress());
        existing.setRecipientName(request.recipientName());
        existing.setRecipientPhone(request.recipientPhone());
        existing.setIdempotencyKey(KEY);
        existing.setRequestHash(orderServiceHash(USER, request));

        when(orderRepository.findByIdempotencyKey(KEY)).thenReturn(Optional.of(existing));
        when(orderLineRepository.findByOrderIdOrderByLineNoAsc("order-01")).thenReturn(List.of());

        CheckoutResponse response = orderService.checkout(USER, KEY, request);

        assertThat(response.orderId()).isEqualTo("order-01");
        // Khong save don moi, khong ghi outbox moi
        verify(orderRepository, never()).save(any(Order.class));
        verify(outboxRepository, never()).save(any(OrderOutbox.class));
    }

    @Test
    @DisplayName("Cung key nhung khac request -> CONFLICT (khong cho doi request khac dung key cu)")
    void checkout_sameKeyDifferentRequest_conflict() {

        Order existing = new Order();
        existing.setId("order-01");
        existing.setUserId(USER);
        existing.setIdempotencyKey(KEY);
        existing.setRequestHash(orderServiceHash(USER, request));

        when(orderRepository.findByIdempotencyKey(KEY)).thenReturn(Optional.of(existing));

        CheckoutRequest different = new CheckoutRequest(
                "456 Le Loi, Q3", "Nguyen Van A", "0901234567", "COD");

        assertThatThrownBy(() -> orderService.checkout(USER, KEY, different))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Idempotency-Key");
    }

    @Test
    @DisplayName("Key cua nguoi khac -> FORBIDDEN (khong leak don nguoi khac qua doan key)")
    void checkout_sameKeyOtherUser_forbidden() {

        Order existing = new Order();
        existing.setId("order-01");
        existing.setUserId("user-02");
        existing.setIdempotencyKey(KEY);
        existing.setRequestHash(orderServiceHash(USER, request));

        when(orderRepository.findByIdempotencyKey(KEY)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> orderService.checkout(USER, KEY, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("không thuộc về bạn");
    }

    @Test
    @DisplayName("Gio trong -> VALIDATION_ERROR, khong tao don")
    void checkout_emptyCart_rejected() {

        when(orderRepository.findByIdempotencyKey(KEY)).thenReturn(Optional.empty());
        when(cartRepository.findByUserIdAndStatus(USER, "ACTIVE"))
                .thenReturn(Optional.of(activeCart()));
        when(cartItemRepository.findByCartIdOrderByCreatedAtAsc("cart-01"))
                .thenReturn(List.of());

        assertThatThrownBy(() -> orderService.checkout(USER, KEY, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Giỏ hàng trống");

        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("Cancel don PENDING_PAYMENT -> CANCELLED + outbox OrderCancelled")
    void cancel_pendingPaymentOrder_succeeds() {

        Order order = new Order();
        order.setId("order-01");
        order.setUserId(USER);
        order.setStatus("PENDING_PAYMENT");
        when(orderRepository.findByIdAndUserId("order-01", USER)).thenReturn(Optional.of(order));
        when(orderLineRepository.findByOrderIdOrderByLineNoAsc("order-01")).thenReturn(List.of());

        OrderResponse response = orderService.cancel(USER, "order-01", "doi y");

        assertThat(response.status()).isEqualTo("CANCELLED");
        assertThat(response.cancelReason()).isEqualTo("doi y");

        ArgumentCaptor<OrderOutbox> outboxCaptor = ArgumentCaptor.forClass(OrderOutbox.class);
        verify(outboxRepository).save(outboxCaptor.capture());
        assertThat(outboxCaptor.getValue().getEventType()).isEqualTo("OrderCancelled");
    }

    @Test
    @DisplayName("Cancel don PAID -> CONFLICT (chi huy duoc don chua thanh toan)")
    void cancel_paidOrder_conflict() {

        Order order = new Order();
        order.setId("order-01");
        order.setUserId(USER);
        order.setStatus("PAID");
        when(orderRepository.findByIdAndUserId("order-01", USER)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancel(USER, "order-01", null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Chỉ hủy được đơn đang chờ thanh toán");
    }

    @Test
    @DisplayName("Ownership: xem don cua nguoi khac -> NOT_FOUND (khong ton tai doi voi minh)")
    void myOrder_otherUsersOrder_notFound() {

        when(orderRepository.findByIdAndUserId("order-01", USER))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.myOrder(USER, "order-01"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    /** Dung chinh hash cua service de build expected — test khong chep cong thuc. */
    private String orderServiceHash(String userId, CheckoutRequest request) {
        try {
            String canonical = userId + "|" + request.shippingAddress() + "|"
                    + request.recipientName() + "|" + request.recipientPhone() + "|"
                    + request.paymentMethod();
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            return java.util.HexFormat.of()
                    .formatHex(digest.digest(canonical.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
