package com.dtthuan3.ecommerce.inventoryservice.controller;

import com.dtthuan3.ecommerce.common.response.ApiResponse;
import com.dtthuan3.ecommerce.inventoryservice.dto.request.ReserveShortageReallocationRequest;
import com.dtthuan3.ecommerce.inventoryservice.dto.response.ShortageReallocationReservationResponse;
import com.dtthuan3.ecommerce.inventoryservice.service.InventoryShortageReallocationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Endpoint giữ hàng ở kho thay thế để bù shortage trước khi giải phóng allocation nguồn. */
@RestController
@RequestMapping("/inventory/shortages/{shortageCaseId}/reallocations")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class InventoryShortageReallocationController {
    private final InventoryShortageReallocationService reallocationService;

    /** Giữ một phần/toàn bộ shortage ở Inventory cùng SKU, enterprise và ghi movement/outbox nguyên tử. */
    @PostMapping("/reserve")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ShortageReallocationReservationResponse> reserve(
            @PathVariable String shortageCaseId,
            @Valid @RequestBody ReserveShortageReallocationRequest request,
            Authentication authentication) {
        String reservedBy = authentication == null ? null : authentication.getName();
        return ApiResponse.ok("Đã giữ hàng thay thế cho shortage case",
                reallocationService.reserve(shortageCaseId, request, reservedBy));
    }
}
