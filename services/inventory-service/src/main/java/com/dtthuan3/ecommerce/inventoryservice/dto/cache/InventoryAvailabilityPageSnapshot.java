package com.dtthuan3.ecommerce.inventoryservice.dto.cache;

import com.dtthuan3.ecommerce.inventoryservice.dto.response.InventoryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

/** Dữ liệu trang tồn khả dụng có cấu trúc ổn định để Redis không phải serialize PageImpl. */
public record InventoryAvailabilityPageSnapshot(
        List<InventoryResponse> content,
        long totalElements
) {
    /** Dựng lại Page Spring từ snapshot sau khi lấy dữ liệu cache. */
    public Page<InventoryResponse> toPage(Pageable pageable) {
        return new PageImpl<>(content, pageable, totalElements);
    }
}
