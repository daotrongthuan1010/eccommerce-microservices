package com.dtthuan3.ecommerce.inventoryservice.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateInventoryRequest(

        /**
         * Doanh nghiệp sở hữu inventory.
         */
        @NotBlank(message = "enterpriseId là bắt buộc")
        String enterpriseId,

        /**
         * Kho chứa inventory.
         */
        @NotBlank(message = "warehouseId là bắt buộc")
        String warehouseId,

        /**
         * Vị trí cụ thể trong kho.
         */
        @NotBlank(message = "locationId là bắt buộc")
        String locationId,

        /**
         * SKU của sản phẩm.
         */
        @NotBlank(message = "SKU là bắt buộc")
        String sku
) {
}
