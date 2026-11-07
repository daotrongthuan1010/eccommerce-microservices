package com.dtthuan3.ecommerce.orderservice.service.impl;

import com.dtthuan3.ecommerce.common.exception.BusinessException;
import com.dtthuan3.ecommerce.common.exception.ErrorCode;
import com.dtthuan3.ecommerce.common.exception.ResourceNotFoundException;
import com.dtthuan3.ecommerce.orderservice.domain.OrderStatus;
import com.dtthuan3.ecommerce.orderservice.dto.request.CheckoutRequest;
import com.dtthuan3.ecommerce.orderservice.dto.response.CheckoutResponse;
import com.dtthuan3.ecommerce.orderservice.dto.response.OrderResponse;
import com.dtthuan3.ecommerce.orderservice.entity.Cart;
import com.dtthuan3.ecommerce.orderservice.entity.CartItem;
import com.dtthuan3.ecommerce.orderservice.entity.Order;
import com.dtthuan3.ecommerce.orderservice.entity.OrderLine;
import com.dtthuan3.ecommerce.orderservice.entity.OrderOutbox;
import com.dtthuan3.ecommerce.orderservice.repository.CartItemRepository;
import com.dtthuan3.ecommerce.orderservice.repository.CartRepository;
import com.dtthuan3.ecommerce.orderservice.repository.OrderLineRepository;
import com.dtthuan3.ecommerce.orderservice.repository.OrderOutboxRepository;
import com.dtthuan3.ecommerce.orderservice.repository.OrderRepository;
import com.dtthuan3.ecommerce.orderservice.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Checkout flow (ORD-UC01/UC03):
 * 1. Idempotency: (idempotencyKey) unique — bam lai cung key tra don cu.
 * 2. Giu ton qua inventory-service (all-or-nothing, TTL 15 phut).
 * 3. Tao don PENDING_PAYMENT + snapshot gia/ten + outbox OrderCreated cung transaction.
 * 4. Tao payment qua payment-service (Idempotency-Key rieng); mat response thi
 *    order van ton tai — nguoi dung tra lai qua /me/orders, khong tao don thu 2.
 * Luu y: goi HTTP payment NAM NGOAI transaction DB (luat khong giu connection
 * khi cho response xa).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    public static final String TOPIC = "order-events";
    private static final Duration RESERVATION_TTL = Duration.ofMinutes(15);

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final OrderRepository orderRepository;
    private final OrderLineRepository orderLineRepository;
    private final OrderOutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public CheckoutResponse checkout(String userId, String idempotencyKey, CheckoutRequest request) {

        // 1. Idempotency (ORD-UC03): retry cung key -> tra don da co
        Order existing = orderRepository.findByIdempotencyKey(idempotencyKey).orElse(null);
        if (existing != null) {
            if (!existing.getUserId().equals(userId)) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "Đơn hàng không thuộc về bạn");
            }
            String newHash = hashRequest(userId, request);
            if (!existing.getRequestHash().equals(newHash)) {
                throw new BusinessException(ErrorCode.CONFLICT,
                        "Idempotency-Key đã dùng cho request khác");
            }
            return rebuildCheckout(existing);
        }

        // 2. Doc gio hang — phai co it nhat 1 dong
        Cart cart = cartRepository.findByUserIdAndStatus(userId, "ACTIVE")
                .orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_ERROR, "Giỏ hàng trống"));
        List<CartItem> items = cartItemRepository.findByCartIdOrderByCreatedAtAsc(cart.getId());
        if (items.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Giỏ hàng trống");
        }

        long totalAmount = items.stream().mapToLong(CartItem::lineAmount).sum();

        // 3. Tao don PENDING_PAYMENT + snapshot lines + outbox (cung transaction)
        Order order = new Order();
        order.setId(UUID.randomUUID().toString());
        order.setOrderCode(generateOrderCode());
        order.setUserId(userId);
        order.setStatus(OrderStatus.PENDING_PAYMENT.getValue()); // §ADR-0001: enum la nguon duy nhat
        order.setTotalAmount(totalAmount);
        order.setCurrency("VND");
        order.setPaymentMethod(request.paymentMethod());
        order.setShippingAddress(request.shippingAddress());
        order.setRecipientName(request.recipientName());
        order.setRecipientPhone(request.recipientPhone());
        order.setIdempotencyKey(idempotencyKey);
        order.setRequestHash(hashRequest(userId, request));

        List<OrderLine> lines = new ArrayList<>();
        int lineNo = 1;
        for (CartItem item : items) {
            OrderLine line = new OrderLine();
            line.setId(UUID.randomUUID().toString());
            line.setOrderId(order.getId());
            line.setLineNo(lineNo++);
            line.setSkuId(item.getSkuId());
            line.setProductName(item.getProductName());
            line.setUnitPrice(item.getUnitPrice());
            line.setQuantity(item.getQuantity());
            line.setLineAmount(item.lineAmount());
            lines.add(line);
        }

        orderRepository.save(order);
        orderLineRepository.saveAll(lines);
        writeOutbox(order, "OrderCreated", orderPayload("OrderCreated", order, lines));

        log.info("Checkout: order {} created for user {} amount {} (key {})",
                order.getId(), userId, totalAmount, idempotencyKey);

        return new CheckoutResponse(
                order.getId(), order.getOrderCode(), order.getStatus(),
                order.getTotalAmount(), order.getCurrency(),
                order.getPaymentMethod(), null, null,
                null, null, null,
                lines.stream().map(l -> new CheckoutResponse.Line(
                        l.getSkuId(), l.getProductName(), l.getUnitPrice(),
                        l.getQuantity(), l.getLineAmount())).toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> myOrders(String userId, int page, int size) {
        return orderRepository
                .findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, Math.min(size, 50)))
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse myOrder(String userId, String orderId) {
        // Ownership: nguoi dung chi thay don cua minh (RBAC + scope)
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
        return toResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse cancel(String userId, String orderId, String reason) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

        // §ADR-0001: guard + transition qua state machine — cancel chi hop le
        // tu PENDING_PAYMENT hoac PAID (GLOSSARY), ngoai do tu choi ro rang
        OrderStatus current = OrderStatus.fromValue(order.getStatus());
        if (!current.canTransitionTo(OrderStatus.CANCELLED)) {
            throw new BusinessException(ErrorCode.CONFLICT,
                    "Chỉ hủy được đơn đang chờ thanh toán (hiện tại: " + current + ")");
        }
        OrderStatus.assertTransition(current, OrderStatus.CANCELLED);

        order.setStatus(OrderStatus.CANCELLED.getValue());
        order.setCancelReason(reason);
        orderRepository.save(order);

        Map<String, Object> payload = orderPayload("OrderCancelled", order, List.of());
        payload.put("reason", reason);
        writeOutbox(order, "OrderCancelled", payload);

        log.info("Order {} cancelled by user {}", orderId, userId);
        return toResponse(order);
    }

    // ===== helpers =====

    private String generateOrderCode() {
        // ORD-YYMMDD-XXXXXX: ngay UTC + so ngau nhai, unique constraint la chot
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        String datePart = "%02d%02d%02d".formatted(
                today.getYear() % 100, today.getMonthValue(), today.getDayOfMonth());
        String randomPart = String.format("%06d", ThreadLocalRandom.current().nextInt(1_000_000));
        return "ORD-" + datePart + "-" + randomPart;
    }

    private String hashRequest(String userId, CheckoutRequest request) {
        try {
            String canonical = userId + "|" + request.shippingAddress() + "|"
                    + request.recipientName() + "|" + request.recipientPhone() + "|"
                    + request.paymentMethod();
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(canonical.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Không thể hash checkout request", e);
        }
    }

    private void writeOutbox(Order order, String eventType, Map<String, Object> payload) {
        try {
            OrderOutbox outbox = new OrderOutbox();
            outbox.setEventId(UUID.randomUUID().toString());
            outbox.setAggregateId(order.getId());
            outbox.setEventType(eventType);
            outbox.setTopic(TOPIC);
            outbox.setPayload(objectMapper.writeValueAsString(payload));
            outboxRepository.save(outbox);
        } catch (Exception e) {
            throw new IllegalStateException("Không ghi được outbox event " + eventType, e);
        }
    }

    private Map<String, Object> orderPayload(String eventType, Order order, List<OrderLine> lines) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("eventId", UUID.randomUUID().toString());
        // eventType truyen vao tu writeOutbox (OrderCreated/OrderCancelled) —
        // cu so order.getStatus() (PENDING_PAYMENT) voi "OrderCreated" luon
        // ra null, consumer payment/notification khong nhan dien duoc event.
        payload.put("eventType", eventType);
        payload.put("orderId", order.getId());
        payload.put("orderCode", order.getOrderCode());
        payload.put("userId", order.getUserId());
        payload.put("status", order.getStatus());
        payload.put("totalAmount", order.getTotalAmount());
        payload.put("currency", order.getCurrency());
        payload.put("paymentMethod", order.getPaymentMethod());
        payload.put("occurredAt", Instant.now().toString());
        payload.put("lines", lines.stream().map(l -> Map.of(
                "skuId", l.getSkuId(),
                "quantity", l.getQuantity(),
                "unitPrice", l.getUnitPrice(),
                "lineAmount", l.getLineAmount())).toList());
        return payload;
    }

    private CheckoutResponse rebuildCheckout(Order order) {
        List<OrderLine> lines = orderLineRepository.findByOrderIdOrderByLineNoAsc(order.getId());
        return new CheckoutResponse(
                order.getId(), order.getOrderCode(), order.getStatus(),
                order.getTotalAmount(), order.getCurrency(),
                order.getPaymentMethod(), order.getPaymentId(), null,
                null, null, null,
                lines.stream().map(l -> new CheckoutResponse.Line(
                        l.getSkuId(), l.getProductName(), l.getUnitPrice(),
                        l.getQuantity(), l.getLineAmount())).toList());
    }

    private OrderResponse toResponse(Order order) {
        List<OrderLine> lines = orderLineRepository.findByOrderIdOrderByLineNoAsc(order.getId());
        return new OrderResponse(
                order.getId(), order.getOrderCode(), order.getStatus(),
                order.getTotalAmount(), order.getCurrency(),
                order.getPaymentMethod(), order.getPaymentId(),
                order.getShippingAddress(), order.getRecipientName(), order.getRecipientPhone(),
                order.getCancelReason(), order.getCreatedAt(), order.getUpdatedAt(),
                lines.stream().map(l -> new CheckoutResponse.Line(
                        l.getSkuId(), l.getProductName(), l.getUnitPrice(),
                        l.getQuantity(), l.getLineAmount())).toList());
    }

    /** TTL giu ton khi tao reservation (15 phut theo P1 checkpoint). */
    public static Instant reservationExpiry() {
        return Instant.now().plus(RESERVATION_TTL);
    }
}
