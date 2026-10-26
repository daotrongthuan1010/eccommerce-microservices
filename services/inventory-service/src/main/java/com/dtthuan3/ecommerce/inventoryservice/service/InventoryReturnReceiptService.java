package com.dtthuan3.ecommerce.inventoryservice.service;

import com.dtthuan3.ecommerce.inventoryservice.dto.request.ReceiveReturnReceiptRequest;
import com.dtthuan3.ecommerce.inventoryservice.dto.response.ReturnReceiptResponse;

public interface InventoryReturnReceiptService {
    /** Lấy trạng thái và dữ liệu biên nhận để tra cứu tiến độ quarantine/inspection. */
    ReturnReceiptResponse getById(String receiptId);

    ReturnReceiptResponse receive(ReceiveReturnReceiptRequest request, String performedBy);
}
