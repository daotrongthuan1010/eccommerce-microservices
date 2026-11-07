package com.dtthuan3.ecommerce.reviewservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Body tạo từ cấm mới. */
public record BannedWordRequest(
        @NotBlank(message = "Từ cấm không được để trống")
        @Size(max = 100, message = "Từ cấm tối đa 100 ký tự")
        String word,

        @Size(max = 255, message = "Ghi chú tối đa 255 ký tự")
        String note
) {
}
