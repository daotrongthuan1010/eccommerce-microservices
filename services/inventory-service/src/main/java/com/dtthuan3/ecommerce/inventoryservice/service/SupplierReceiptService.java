package com.dtthuan3.ecommerce.inventoryservice.service;

import com.dtthuan3.ecommerce.inventoryservice.dto.request.CreateSupplierReceiptRequest;
import com.dtthuan3.ecommerce.inventoryservice.dto.request.UpdateSupplierReceiptActualsRequest;
import com.dtthuan3.ecommerce.inventoryservice.dto.response.SupplierReceiptResponse;

/** Cổng nghiệp vụ cho các thao tác của phiếu nhập nhà cung cấp. */
public interface SupplierReceiptService {
    /** Tạo phiếu nhập DRAFT hoặc trả lại kết quả cũ nếu operationId được retry cùng payload. */
    SupplierReceiptResponse create(String warehouseId, CreateSupplierReceiptRequest request);

    /** Cập nhật số lượng thực nhận/phân loại trên phiếu DRAFT theo optimistic version. */
    SupplierReceiptResponse updateActuals(String receiptId, UpdateSupplierReceiptActualsRequest request);
}
