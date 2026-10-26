package com.dtthuan3.ecommerce.inventoryservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Dữ liệu mở phiên kiểm kê và xác định phạm vi tồn cần chụp snapshot. */
public record OpenInventoryCountSessionRequest(
        @NotBlank(message = "enterpriseId không được để trống")
        @Size(max = 100, message = "enterpriseId tối đa 100 ký tự") String enterpriseId,
        @NotBlank(message = "warehouseId không được để trống")
        @Size(max = 36, message = "warehouseId tối đa 36 ký tự") String warehouseId,
        @Size(max = 36, message = "locationId tối đa 36 ký tự")
        String locationId,
        @Size(max = 100, message = "sku tối đa 100 ký tự")
        String sku,
        @NotBlank(message = "operationId không được để trống")
        @Size(max = 100, message = "operationId tối đa 100 ký tự") String operationId,
        @NotBlank(message = "reason không được để trống")
        @Size(max = 500, message = "reason tối đa 500 ký tự") String reason
) {}
