package com.dtthuan3.ecommerce.inventoryservice.service;

import com.dtthuan3.ecommerce.inventoryservice.dto.request.InspectReturnReceiptRequest;
import com.dtthuan3.ecommerce.inventoryservice.dto.response.ReturnInspectionResponse;

/**
 * Hợp đồng xử lý phân loại chất lượng cho parcel hàng khách trả.
 * Interface này công bố các use case Inventory cho controller, worker và adapter gọi tới. Class triển khai
 * nằm ở service/impl, vì vậy bên gọi phụ thuộc vào hợp đồng thay vì chi tiết xử lý.
 */
public interface InventoryReturnInspectionService {

    /** Tra cứu kết quả đã chốt sau kiểm định để caller khôi phục response khi mất kết nối. */
    ReturnInspectionResponse getByReceiptId(String receiptId);

    /** Phân loại toàn bộ parcel và chuyển số lượng khỏi bucket chờ kiểm tra. */
    ReturnInspectionResponse inspect(String receiptId, InspectReturnReceiptRequest request,
                                     String inspectedBy);
}
