package com.dtthuan3.ecommerce.inventoryservice.integration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.dtthuan3.ecommerce.inventoryservice.integration.event.ProductSkuEvent;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Nhận event Product và chuyển cho lớp nghiệp vụ cập nhật projection SKU cục bộ.
 * Consumer này không sở hữu dữ liệu gốc và không ghi trực tiếp vào bảng nghiệp vụ tồn kho.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryProjectionKafkaListener {

    private final ObjectMapper objectMapper;
    private final InventoryProjectionEventService projectionEventService;
    private final MeterRegistry meterRegistry;

    /** Đồng bộ trạng thái SKU từ Product Service để giảm các lần gọi validate đồng bộ. */
    @KafkaListener(topics = "${inventory.kafka.product-events-topic:product-events}")
    public void onProductEvent(String payload) {
        Timer.Sample processingTimer = Timer.start(meterRegistry);
        String outcome = "success";
        meterRegistry.counter("inventory.kafka.product_events.received").increment();
        try {
            ProductSkuEvent event = readEvent(payload, ProductSkuEvent.class);
            // Topic có thể chứa thêm event Product khác; chỉ xử lý hợp đồng projection SKU đã đăng ký.
            if (!"SkuCreated".equals(event.eventType()) && !"SkuDisabled".equals(event.eventType())) {
                meterRegistry.counter("inventory.kafka.product_events.unsupported").increment();
                log.debug("Bỏ qua Product event không thuộc projection SKU: {}", event.eventType());
                return;
            }
            InventoryProjectionEventService.ApplyOutcome applyOutcome = projectionEventService.applyProductEvent(event);
            log.debug("Product event xử lý xong: eventId={}, sku={}, outcome={}",
                    event.eventId(), event.skuCode(), applyOutcome);
        } catch (RuntimeException failure) {
            outcome = "failure";
            meterRegistry.counter("inventory.kafka.product_events.failures").increment();
            throw failure;
        } finally {
            processingTimer.stop(Timer.builder("inventory.kafka.product_events.processing")
                    .tag("outcome", outcome).register(meterRegistry));
        }
    }

    /** Payload dùng JSON phẳng theo hợp đồng INV-01; payload sai định dạng được đẩy cho listener container xử lý lỗi. */
    private <T> T readEvent(String payload, Class<T> eventType) {
        try {
            return objectMapper.readValue(payload, eventType);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Không thể đọc payload Kafka thành event Inventory", exception);
        }
    }
}
