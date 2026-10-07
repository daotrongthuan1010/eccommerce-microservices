package com.dtthuan3.ecommerce.inventoryservice.config;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Cấu hình giới hạn kích thước batch inventory. */
@Validated
@ConfigurationProperties(prefix = "inventory.batch")
public record InventoryBatchProperties(
        @Min(value = 1, message = "inventory.batch.max-size phải lớn hơn 0")
        int maxSize
) {
}
