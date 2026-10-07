package com.dtthuan3.ecommerce.inventoryservice.dto.response;

/** Kết quả chuyển kho gồm trạng thái mới của cả inventory nguồn và inventory đích. */
public record TransferInventoryResponse(
        /** Inventory đã bị trừ số lượng. */
        InventoryResponse source,
        /** Inventory đã được cộng số lượng. */
        InventoryResponse destination,
        /** Loại chứng từ dùng để đối soát hai movement. */
        String referenceType,
        /** Mã tham chiếu chung của movement TRANSFER_OUT và TRANSFER_IN. */
        String referenceId
) {
}
