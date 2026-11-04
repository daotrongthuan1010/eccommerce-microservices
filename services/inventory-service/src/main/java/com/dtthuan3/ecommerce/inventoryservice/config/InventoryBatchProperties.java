package com.dtthuan3.ecommerce.inventoryservice.config;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Cấu hình giới hạn kích thước batch inventory.
 * Class này gom cấu hình hạ tầng hoặc thuộc tính runtime để Spring khởi tạo service nhất quán; thay đổi cấu
 * hình không cần đưa logic cấu hình vào controller hay use case.
 */
@Validated
@ConfigurationProperties(prefix = "inventory.batch")
public record InventoryBatchProperties(
        @Min(value = 1, message = "inventory.batch.max-size phải ít nhất là 1")
        int maxSize
) {
}
