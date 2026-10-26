package com.dtthuan3.ecommerce.inventoryservice.service;

import com.dtthuan3.ecommerce.inventoryservice.dto.request.CreateInventoryTransferRequest;
import com.dtthuan3.ecommerce.inventoryservice.dto.request.DispatchInventoryTransferRequest;
import com.dtthuan3.ecommerce.inventoryservice.dto.request.ApproveInventoryTransferRequest;
import com.dtthuan3.ecommerce.inventoryservice.dto.request.ReceiveInventoryTransferRequest;
import com.dtthuan3.ecommerce.inventoryservice.dto.response.InventoryTransferResponse;
import com.dtthuan3.ecommerce.inventoryservice.dto.response.InventoryTransferDispatchResponse;
import com.dtthuan3.ecommerce.inventoryservice.dto.response.InventoryTransferReceiveResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/** Use case phiếu điều chuyển; giao diện không phụ thuộc controller hoặc adapter lưu trữ. */
public interface InventoryTransferService {
    /** Đọc phiếu và danh sách dòng để client lấy trạng thái/version mới nhất trước thao tác tiếp theo. */
    InventoryTransferResponse getById(String transferId);

    /** Liệt kê phiếu theo enterprise, có thể lọc status và giới hạn kết quả theo trang. */
    Page<InventoryTransferResponse> list(String enterpriseId, String status, Pageable pageable);

    /** Tạo phiếu CREATED có idempotency; chưa thay đổi số dư kho nguồn hoặc kho đích. */
    InventoryTransferResponse create(CreateInventoryTransferRequest request, String createdBy);

    /** Duyệt phiếu CREATED trước khi dispatch; người duyệt phải khác người tạo. */
    InventoryTransferResponse approve(String transferId, ApproveInventoryTransferRequest request,
                                      String approvedBy);

    /** Xuất toàn bộ phiếu một lần; cập nhật tồn nguồn, in-transit, movement và trạng thái nguyên tử. */
    InventoryTransferDispatchResponse dispatch(String transferId,
                                               DispatchInventoryTransferRequest request,
                                               String dispatchedBy);

    /** Nhận một phần/toàn bộ hàng đang chuyển, ghi sai lệch và cập nhật tồn hai đầu nguyên tử. */
    InventoryTransferReceiveResponse receive(String transferId,
                                              ReceiveInventoryTransferRequest request,
                                              String receivedBy);
}
