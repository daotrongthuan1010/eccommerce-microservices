package com.dtthuan3.ecommerce.inventoryservice.service;

import com.dtthuan3.ecommerce.inventoryservice.dto.request.InspectReturnReceiptRequest;
import com.dtthuan3.ecommerce.inventoryservice.dto.response.ReturnInspectionResponse;

/** Hợp đồng xử lý phân loại chất lượng cho parcel hàng khách trả. */
public interface InventoryReturnInspectionService {

    /** Phân loại toàn bộ parcel và chuyển số lượng khỏi bucket chờ kiểm tra. */
    ReturnInspectionResponse inspect(String receiptId, InspectReturnReceiptRequest request,
                                     String inspectedBy);
}
