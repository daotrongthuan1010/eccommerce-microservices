package com.dtthuan3.ecommerce.reviewservice.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;
import java.util.List;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewRequest {

    // Dùng cho API Create (Tạo mới)
    private String lineId;

    // --- Dùng chung cho cả Create và Update ---
    @NotNull(message = "Số sao không được để trống")
    @Min(value = 1, message = "Số sao tối thiểu là 1")
    @Max(value = 5, message = "Số sao tối đa là 5")
    private Integer rating;

    @Size(max = 3000, message = "Nội dung không được vượt quá 3000 ký tự")
    private String content;

    @Valid
    private List<ReviewMediaRequest> media;

    // Tùy chọn ẩn danh (true = che tên, false = hiện tên thật)
    private Boolean isAnonymous;

    // --- Dùng riêng cho API Update (Optimistic Locking) ---
    private Long version;
}
