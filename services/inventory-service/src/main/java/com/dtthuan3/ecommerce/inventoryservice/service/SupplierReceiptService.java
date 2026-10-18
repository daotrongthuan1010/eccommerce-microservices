package com.dtthuan3.ecommerce.inventoryservice.service;

import com.dtthuan3.ecommerce.inventoryservice.dto.request.CreateSupplierReceiptRequest;
import com.dtthuan3.ecommerce.inventoryservice.dto.request.UpdateSupplierReceiptActualsRequest;
import com.dtthuan3.ecommerce.inventoryservice.dto.response.SupplierReceiptResponse;
import com.dtthuan3.ecommerce.inventoryservice.dto.response.SupplierReceiptSummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/** Cổng nghiệp vụ cho các thao tác của phiếu nhập nhà cung cấp. */
public interface SupplierReceiptService {
    /** Đọc header, các dòng hàng và chứng từ đính kèm của một phiếu nhập. */
    SupplierReceiptResponse getById(String receiptId);

    /** Liệt kê phiếu theo kho với phân trang và dữ liệu tóm tắt. */
    Page<SupplierReceiptSummaryResponse> listByWarehouse(String warehouseId, Pageable pageable);

    /** Tạo phiếu nhập DRAFT hoặc trả lại kết quả cũ nếu operationId được retry cùng payload. */
    SupplierReceiptResponse create(String warehouseId, CreateSupplierReceiptRequest request);

    /** Cập nhật số lượng thực nhận/phân loại trên phiếu DRAFT theo optimistic version. */
    SupplierReceiptResponse updateActuals(String receiptId, UpdateSupplierReceiptActualsRequest request);
}
