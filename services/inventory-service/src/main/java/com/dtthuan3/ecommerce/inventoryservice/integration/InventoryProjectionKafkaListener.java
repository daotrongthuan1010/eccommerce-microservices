package com.dtthuan3.ecommerce.inventoryservice.integration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.dtthuan3.ecommerce.inventoryservice.integration.event.ProductSkuEvent;
import com.dtthuan3.ecommerce.inventoryservice.integration.event.WarehouseStatusChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Nhận event từ Product/Warehouse và chuyển cho lớp nghiệp vụ cập nhật projection cục bộ.
 * Consumer này không sở hữu dữ liệu gốc và không ghi trực tiếp vào bảng nghiệp vụ tồn kho.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryProjectionKafkaListener {

    private final ObjectMapper objectMapper;
    private final InventoryProjectionEventService projectionEventService;

    /** Đồng bộ trạng thái SKU từ Product Service để giảm các lần gọi validate đồng bộ. */
    @KafkaListener(topics = "${inventory.kafka.product-events-topic:product-events}")
    public void onProductEvent(String payload) {
        ProductSkuEvent event = readEvent(payload, ProductSkuEvent.class);
        // Topic có thể chứa thêm event Product khác; chỉ xử lý event nằm trong hợp đồng projection SKU.
        if (!"SkuCreated".equals(event.eventType()) && !"SkuDisabled".equals(event.eventType())) {
            log.debug("Bỏ qua Product event không thuộc projection SKU: {}", event.eventType());
            return;
        }
        projectionEventService.applyProductEvent(event);
    }

    /** Đồng bộ trạng thái/enterprise của Warehouse mà không tạo foreign key sang database service khác. */
    @KafkaListener(topics = "${inventory.kafka.warehouse-events-topic:warehouse-events}")
    public void onWarehouseEvent(String payload) {
        WarehouseStatusChangedEvent event = readEvent(payload, WarehouseStatusChangedEvent.class);
        // Bỏ qua các event khác trên cùng topic để consumer có thể mở rộng mà không ảnh hưởng projection hiện tại.
        if (!"WarehouseStatusChanged".equals(event.eventType())) {
            log.debug("Bỏ qua Warehouse event không thuộc projection trạng thái kho: {}", event.eventType());
            return;
        }
        projectionEventService.applyWarehouseEvent(event);
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
