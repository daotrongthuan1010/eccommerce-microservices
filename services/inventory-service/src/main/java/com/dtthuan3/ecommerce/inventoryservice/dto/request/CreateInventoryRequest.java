package com.dtthuan3.ecommerce.inventoryservice.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateInventoryRequest(

        /**
         * Doanh nghiệp sở hữu inventory.
         */
        @NotBlank(message = "Enterprise id is required")
        String enterpriseId,

        /**
         * Kho chứa inventory.
         */
        @NotBlank(message = "Warehouse id is required")
        String warehouseId,

        /**
         * Vị trí cụ thể trong kho.
         */
        @NotBlank(message = "Location id is required")
        String locationId,

        /**
         * SKU của sản phẩm.
         */
        @NotBlank(message = "SKU is required")
        String sku
) {
}
