package com.dtthuan3.ecommerce.inventoryservice.port;

import java.util.Collection;

/** Cổng xác minh các chứng từ của phiếu đã được tải lên File Service. */
public interface ReceiptDocumentValidator {
    /** Ném BusinessException nếu có chứng từ không tồn tại hoặc File Service không khả dụng. */
    void requireUploaded(Collection<String> assetIds, String bearerToken);
}
