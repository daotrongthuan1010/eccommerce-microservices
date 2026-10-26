package com.dtthuan3.ecommerce.inventoryservice.service;

import com.dtthuan3.ecommerce.inventoryservice.dto.request.ReportInventoryShortageRequest;
import com.dtthuan3.ecommerce.inventoryservice.dto.response.InventoryShortageCaseResponse;
import com.dtthuan3.ecommerce.inventoryservice.dto.response.InventoryShortageCaseDetailResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/** Use case tiếp nhận shortage được kho phát hiện khi soạn allocation đã commit. */
public interface InventoryShortageService {
    InventoryShortageCaseResponse report(ReportInventoryShortageRequest request, String reportedBy);
    InventoryShortageCaseDetailResponse getById(String caseId);
    Page<InventoryShortageCaseResponse> list(String enterpriseId, String status, Pageable pageable);
}
