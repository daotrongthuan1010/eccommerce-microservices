package com.dtthuan3.ecommerce.inventoryservice.service;

import com.dtthuan3.ecommerce.inventoryservice.dto.request.ReceiveReturnReceiptRequest;
import com.dtthuan3.ecommerce.inventoryservice.dto.response.ReturnReceiptResponse;

/**
 * Hợp đồng nghiệp vụ nhận và tra cứu biên nhận hàng khách trả trước khi kiểm định chất lượng.
 * Interface này công bố các use case Inventory cho controller, worker và adapter gọi tới. Class triển khai
 * nằm ở service/impl, vì vậy bên gọi phụ thuộc vào hợp đồng thay vì chi tiết xử lý.
 */
public interface InventoryReturnReceiptService {
    /** Lấy trạng thái và dữ liệu biên nhận để tra cứu tiến độ quarantine/inspection. */
    ReturnReceiptResponse getById(String receiptId);

    ReturnReceiptResponse receive(ReceiveReturnReceiptRequest request, String performedBy);
}
