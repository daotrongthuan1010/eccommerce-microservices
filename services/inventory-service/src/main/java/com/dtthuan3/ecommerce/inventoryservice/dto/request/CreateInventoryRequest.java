package com.dtthuan3.ecommerce.inventoryservice.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Dữ liệu đầu vào để tạo bucket tồn ban đầu theo enterprise, warehouse, location và SKU.
 * Record này là hợp đồng dữ liệu đầu vào giữa client và Inventory. Các trường thể hiện thông tin cần cho
 * use case; validation ngăn dữ liệu thiếu hoặc sai trước khi service xử lý và record không đại diện cho
 * bảng database.
 */
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
