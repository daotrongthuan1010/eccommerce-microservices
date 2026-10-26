package com.dtthuan3.ecommerce.inventoryservice.dto.cache;

import com.dtthuan3.ecommerce.inventoryservice.dto.response.LocationResponse;

import java.util.List;

/** Snapshot có thể serialize an toàn vào Redis thay vì lưu trực tiếp đối tượng Page của Spring Data. */
public record LocationPageSnapshot(List<LocationResponse> content, int page, int size, long totalElements) {
    /** Tính tổng trang từ metadata và số phần tử, không cần lưu đối tượng PageImpl vào cache. */
    public int totalPages() {
        return size == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
    }
}
