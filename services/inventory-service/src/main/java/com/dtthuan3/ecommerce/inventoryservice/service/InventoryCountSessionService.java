package com.dtthuan3.ecommerce.inventoryservice.service;

import com.dtthuan3.ecommerce.inventoryservice.dto.request.OpenInventoryCountSessionRequest;
import com.dtthuan3.ecommerce.inventoryservice.dto.request.RecordInventoryCountRequest;
import com.dtthuan3.ecommerce.inventoryservice.dto.request.RequestInventoryCountRecountRequest;
import com.dtthuan3.ecommerce.inventoryservice.dto.response.InventoryCountSessionResponse;
import com.dtthuan3.ecommerce.inventoryservice.dto.response.InventoryCountSessionLineResponse;
import com.dtthuan3.ecommerce.inventoryservice.dto.response.InventoryCountSubmissionResponse;
import com.dtthuan3.ecommerce.inventoryservice.dto.response.InventoryCountSessionSubmitResponse;
import com.dtthuan3.ecommerce.inventoryservice.dto.response.InventoryCountSessionApprovalResponse;
import com.dtthuan3.ecommerce.inventoryservice.dto.response.InventoryCountRecountResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/** Use case mở phiên kiểm kê và ghi nhận baseline trước khi nhân viên nhập số đếm. */
public interface InventoryCountSessionService {
    Page<InventoryCountSessionResponse> list(String enterpriseId, String warehouseId,
                                              String status, Pageable pageable);
    InventoryCountSessionResponse open(OpenInventoryCountSessionRequest request, String openedBy);
    Page<InventoryCountSessionLineResponse> getLines(String sessionId, int page, int size);
    InventoryCountSubmissionResponse recordCounts(String sessionId, RecordInventoryCountRequest request,
                                                   String recordedBy);
    InventoryCountSessionSubmitResponse submit(String sessionId);
    InventoryCountSessionApprovalResponse approve(String sessionId, String approvedBy);
    InventoryCountRecountResponse requestRecount(String sessionId, RequestInventoryCountRecountRequest request,
                                                  String requestedBy);
}
