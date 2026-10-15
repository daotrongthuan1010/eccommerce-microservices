package com.dtthuan3.ecommerce.inventoryservice.integration;

import com.dtthuan3.ecommerce.inventoryservice.entity.SkuCatalogProjection;
import com.dtthuan3.ecommerce.inventoryservice.integration.event.ProductSkuEvent;
import com.dtthuan3.ecommerce.inventoryservice.repository.SkuCatalogProjectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/** Áp dụng event Product vào bảng projection trong transaction độc lập với dữ liệu tồn kho. */
@Service
@RequiredArgsConstructor
public class InventoryProjectionEventService {

    private final SkuCatalogProjectionRepository skuRepository;

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

    private boolean isOlder(Long incomingVersion, Instant incomingTime, Long currentVersion, Instant currentTime) {
        if (incomingVersion != null && currentVersion != null) {
            return incomingVersion <= currentVersion;
        }
        // Khi thiếu version, dùng thời gian nguồn để loại event đến muộn.
        return incomingTime != null && currentTime != null && incomingTime.isBefore(currentTime);
    }
}
