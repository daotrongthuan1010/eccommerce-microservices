package com.dtthuan3.ecommerce.reviewservice.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModerationRequest {

    @NotNull(message = "Quyết định kiểm duyệt không được để trống")
    private Boolean isApproved;

    // Mã lỗi hệ thống (VD: "SPAM", "PROFANITY", "INAPPROPRIATE_IMAGE")
    private String reasonCode;

    // Ghi chú chi tiết của Admin (VD: "Khách hàng chửi bậy ở đoạn 2")
    private String reasonNote;
}