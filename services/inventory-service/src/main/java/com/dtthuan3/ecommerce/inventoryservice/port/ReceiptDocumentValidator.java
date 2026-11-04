package com.dtthuan3.ecommerce.inventoryservice.port;

import java.util.Collection;

/**
 * Cổng xác minh các chứng từ của phiếu đã được tải lên File Service.
 * Interface này định nghĩa cổng tích hợp mà use case Inventory phụ thuộc. Adapter cụ thể có thể thay đổi mà
 * service không phải phụ thuộc trực tiếp thư viện hoặc giao thức bên ngoài.
 */
public interface ReceiptDocumentValidator {
    /** Ném BusinessException nếu có chứng từ không tồn tại hoặc File Service không khả dụng. */
    void requireUploaded(Collection<String> assetIds, String bearerToken);
}
