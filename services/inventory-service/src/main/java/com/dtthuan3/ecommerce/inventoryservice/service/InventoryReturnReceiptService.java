package com.dtthuan3.ecommerce.inventoryservice.service;

import com.dtthuan3.ecommerce.inventoryservice.dto.request.ReceiveReturnReceiptRequest;
import com.dtthuan3.ecommerce.inventoryservice.dto.response.ReturnReceiptResponse;

public interface InventoryReturnReceiptService {
    ReturnReceiptResponse receive(ReceiveReturnReceiptRequest request, String performedBy);
}
