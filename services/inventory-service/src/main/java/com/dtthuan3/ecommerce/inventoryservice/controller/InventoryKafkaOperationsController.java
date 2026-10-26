package com.dtthuan3.ecommerce.inventoryservice.controller;

import com.dtthuan3.ecommerce.common.response.ApiResponse;
import com.dtthuan3.ecommerce.inventoryservice.dto.request.ReplayProductEventDltRequest;
import com.dtthuan3.ecommerce.inventoryservice.dto.response.ReplayProductEventDltResponse;
import com.dtthuan3.ecommerce.inventoryservice.service.InventoryDltReplayService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Endpoint quản trị Kafka của riêng Inventory; chỉ ADMIN được phát lại record lỗi. */
@RestController
@RequestMapping("/inventory/operations/kafka")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class InventoryKafkaOperationsController {
    private final InventoryDltReplayService dltReplayService;

    /** Replay đúng một offset; API không nhận payload nên không thể âm thầm sửa nội dung event gốc. */
    @PostMapping("/product-events-dlt/replay")
    public ApiResponse<ReplayProductEventDltResponse> replayProductEvent(
            @Valid @RequestBody ReplayProductEventDltRequest request,
            Authentication authentication) {
        String operator = authentication == null ? "unknown" : authentication.getName();
        return ApiResponse.ok("Đã gửi lại Product event từ DLT",
                dltReplayService.replayProductEvent(request, operator));
    }
}
