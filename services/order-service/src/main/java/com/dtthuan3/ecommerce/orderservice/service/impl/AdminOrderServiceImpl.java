package com.dtthuan3.ecommerce.orderservice.service.impl;

import com.dtthuan3.ecommerce.common.exception.BusinessException;
import com.dtthuan3.ecommerce.common.exception.ErrorCode;
import com.dtthuan3.ecommerce.common.exception.ResourceNotFoundException;
import com.dtthuan3.ecommerce.orderservice.domain.OrderStatus;
import com.dtthuan3.ecommerce.orderservice.dto.response.CheckoutResponse;
import com.dtthuan3.ecommerce.orderservice.dto.response.OrderResponse;
import com.dtthuan3.ecommerce.orderservice.entity.Order;
import com.dtthuan3.ecommerce.orderservice.entity.OrderLine;
import com.dtthuan3.ecommerce.orderservice.repository.OrderLineRepository;
import com.dtthuan3.ecommerce.orderservice.repository.OrderRepository;
import com.dtthuan3.ecommerce.orderservice.service.AdminOrderService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Quan tri don hang (ADMIN): list toan bo don, chuyen trang thai fulfill,
 * huy don non-terminal. TAI DUNG state machine OrderStatus (§ADR-0001) —
 * khong tu che transition map rieng: CONFIRMED/SHIPPED la viec cua admin
 * fulfill, CANCELLED chi cho don chua xac nhan (don PAID phai di hoan tien).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminOrderServiceImpl implements AdminOrderService {

    private final OrderRepository orderRepository;
    private final OrderLineRepository orderLineRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> list(int page, int size, String status, String search) {
        return orderRepository
                .search(status, normalize(search),
                        PageRequest.of(page, Math.min(Math.max(size, 1), 100)))
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse detail(String orderId) {
        return toResponse(find(orderId));
    }

    @Override
    @Transactional
    public OrderResponse updateStatus(String orderId, String status) {
        Order order = find(orderId);
        OrderStatus target = parseStatus(status);
        OrderStatus current = parseStatus(order.getStatus());
        if (current == null || target == null) {
            throw new BusinessException(ErrorCode.CONFLICT,
                    "Trạng thái không hợp lệ: " + order.getStatus() + " -> " + status);
        }
        if (!current.canTransitionTo(target)) {
            throw new BusinessException(ErrorCode.CONFLICT,
                    "Không thể chuyển từ " + current + " sang " + target);
        }
        order.setStatus(target.getValue());
        orderRepository.save(order);
        log.info("Order {} status {} -> {} by admin", orderId, current, target);
        return toResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse cancel(String orderId, String reason) {
        Order order = find(orderId);
        OrderStatus current = parseStatus(order.getStatus());
        if (current == null || !current.canTransitionTo(OrderStatus.CANCELLED)) {
            throw new BusinessException(ErrorCode.CONFLICT,
                    "Không thể hủy đơn ở trạng thái " + order.getStatus()
                    + " (đơn đã thanh toán phải hoàn tiền, không hủy trực tiếp)");
        }
        order.setStatus(OrderStatus.CANCELLED.getValue());
        order.setCancelReason(reason);
        orderRepository.save(order);
        log.info("Order {} cancelled by admin", orderId);
        return toResponse(order);
    }

    private Order find(String orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
    }

    /** Parse an toan: null khi gia tri DB ngoai enum (legacy) — xu ly o caller. */
    private static OrderStatus parseStatus(String status) {
        try {
            return OrderStatus.fromValue(status);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static String normalize(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
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
}
