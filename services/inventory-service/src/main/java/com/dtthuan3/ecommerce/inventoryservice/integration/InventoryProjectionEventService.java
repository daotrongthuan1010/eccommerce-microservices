package com.dtthuan3.ecommerce.inventoryservice.integration;

import com.dtthuan3.ecommerce.inventoryservice.entity.SkuCatalogProjection;
import com.dtthuan3.ecommerce.inventoryservice.entity.WarehouseReferenceProjection;
import com.dtthuan3.ecommerce.inventoryservice.integration.event.ProductSkuEvent;
import com.dtthuan3.ecommerce.inventoryservice.integration.event.WarehouseStatusChangedEvent;
import com.dtthuan3.ecommerce.inventoryservice.repository.SkuCatalogProjectionRepository;
import com.dtthuan3.ecommerce.inventoryservice.repository.WarehouseReferenceProjectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;

/** Áp dụng event vào các bảng projection trong transaction độc lập với dữ liệu tồn kho. */
@Service
@RequiredArgsConstructor
public class InventoryProjectionEventService {

    private final SkuCatalogProjectionRepository skuRepository;
    private final WarehouseReferenceProjectionRepository warehouseRepository;

    @Transactional
    public void applyProductEvent(ProductSkuEvent event) {
        // Kiểm tra envelope tối thiểu trước khi truy cập DB để không tạo projection thiếu khóa định danh.
        if (event.eventId() == null || event.eventId().isBlank()
                || event.eventType() == null || event.skuCode() == null || event.skuCode().isBlank()) {
            throw new IllegalArgumentException("Product event phải có eventId, eventType và skuCode");
        }
        if (event.version() != null && event.version() < 0) {
            throw new IllegalArgumentException("version của Product event không được âm");
        }

        boolean active = switch (event.eventType()) {
            case "SkuCreated" -> true;
            case "SkuDisabled" -> false;
            default -> throw new IllegalArgumentException("Không hỗ trợ Product event: " + event.eventType());
        };

        String skuCode = event.skuCode().trim();
        SkuCatalogProjection current = skuRepository.findById(skuCode).orElse(null);
        // Version của nguồn là thứ tự ưu tiên; occurredAt được dùng khi một trong hai bản ghi thiếu version.
        // Event cùng version cũng bị bỏ qua để xử lý lặp không ghi đè projection hiện có.
        if (isOlder(event.version(), event.occurredAt(),
                current == null ? null : current.getSourceVersion(),
                current == null ? null : current.getSourceUpdatedAt())) {
            return;
        }

        // Ghi thời điểm nhận làm mốc dự phòng nếu producer chưa gửi occurredAt.
        Instant occurredAt = event.occurredAt() == null ? Instant.now() : event.occurredAt();
        skuRepository.save(new SkuCatalogProjection(skuCode, active, event.version(), occurredAt));
    }

    @Transactional
    public void applyWarehouseEvent(WarehouseStatusChangedEvent event) {
        // Warehouse projection cần đủ warehouseId và enterpriseId để Inventory kiểm tra ownership khi ghi tồn.
        if (event.eventId() == null || event.eventId().isBlank()
                || event.eventType() == null || !"WarehouseStatusChanged".equals(event.eventType())
                || event.warehouseId() == null || event.warehouseId().isBlank()
                || event.enterpriseId() == null || event.enterpriseId().isBlank()
                || event.status() == null || event.status().isBlank()) {
            throw new IllegalArgumentException(
                    "WarehouseStatusChanged phải có eventId, warehouseId, enterpriseId và status");
        }
        if (event.version() != null && event.version() < 0) {
            throw new IllegalArgumentException("version của Warehouse event không được âm");
        }

        String status = event.status().trim().toUpperCase(Locale.ROOT);
        if (!status.equals("ACTIVE") && !status.equals("INACTIVE")) {
            throw new IllegalArgumentException("status của Warehouse chỉ nhận ACTIVE hoặc INACTIVE");
        }

        String warehouseId = event.warehouseId().trim();
        WarehouseReferenceProjection current = warehouseRepository.findById(warehouseId).orElse(null);
        // Không cho event cũ quay ngược trạng thái ACTIVE/INACTIVE đã nhận trước đó.
        if (isOlder(event.version(), event.occurredAt(),
                current == null ? null : current.getSourceVersion(),
                current == null ? null : current.getSourceUpdatedAt())) {
            return;
        }

        Instant occurredAt = event.occurredAt() == null ? Instant.now() : event.occurredAt();
        warehouseRepository.save(new WarehouseReferenceProjection(
                warehouseId, event.enterpriseId().trim(), status, event.version(), occurredAt));
    }

    private boolean isOlder(Long incomingVersion, Instant incomingTime, Long currentVersion, Instant currentTime) {
        if (incomingVersion != null && currentVersion != null) {
            return incomingVersion <= currentVersion;
        }
        // Khi thiếu version, dùng thời gian nguồn để loại event đến muộn; nếu cũng thiếu timestamp,
        // Kafka cần giữ thứ tự theo aggregate key (skuCode/warehouseId) ở phía producer.
        return incomingTime != null && currentTime != null && incomingTime.isBefore(currentTime);
    }
}
