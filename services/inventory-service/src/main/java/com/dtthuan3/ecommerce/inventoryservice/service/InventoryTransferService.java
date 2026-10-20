package com.dtthuan3.ecommerce.inventoryservice.service;

import com.dtthuan3.ecommerce.inventoryservice.dto.request.CreateInventoryTransferRequest;
import com.dtthuan3.ecommerce.inventoryservice.dto.request.DispatchInventoryTransferRequest;
import com.dtthuan3.ecommerce.inventoryservice.dto.request.ReceiveInventoryTransferRequest;
import com.dtthuan3.ecommerce.inventoryservice.dto.response.InventoryTransferResponse;
import com.dtthuan3.ecommerce.inventoryservice.dto.response.InventoryTransferDispatchResponse;
import com.dtthuan3.ecommerce.inventoryservice.dto.response.InventoryTransferReceiveResponse;

/** Use case phiếu điều chuyển; giao diện không phụ thuộc controller hoặc adapter lưu trữ. */
public interface InventoryTransferService {
    /** Tạo phiếu CREATED có idempotency; chưa thay đổi số dư kho nguồn hoặc kho đích. */
    InventoryTransferResponse create(CreateInventoryTransferRequest request, String createdBy);

    /** Xuất toàn bộ phiếu một lần; cập nhật tồn nguồn, in-transit, movement và trạng thái nguyên tử. */
    InventoryTransferDispatchResponse dispatch(String transferId,
                                               DispatchInventoryTransferRequest request,
                                               String dispatchedBy);

    /** Nhận một phần/toàn bộ hàng đang chuyển, ghi sai lệch và cập nhật tồn hai đầu nguyên tử. */
    InventoryTransferReceiveResponse receive(String transferId,
                                              ReceiveInventoryTransferRequest request,
                                              String receivedBy);
}
