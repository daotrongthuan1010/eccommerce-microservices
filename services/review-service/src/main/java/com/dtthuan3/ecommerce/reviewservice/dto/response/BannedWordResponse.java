package com.dtthuan3.ecommerce.reviewservice.dto.response;

import java.time.LocalDateTime;

/** Một từ cấm trong danh sách quản trị. */
public record BannedWordResponse(
        Long id,
        String word,
        String note,
        LocalDateTime createdAt
) {
}
