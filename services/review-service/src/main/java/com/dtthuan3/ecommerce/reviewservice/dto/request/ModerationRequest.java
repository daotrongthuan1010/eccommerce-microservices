package com.dtthuan3.ecommerce.reviewservice.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModerationRequest {

    @NotNull(message = "Quyết định kiểm duyệt không được để trống")
    private Boolean isApproved;

    /** Bắt buộc khi từ chối (SPAM, PROFANITY, INAPPROPRIATE_IMAGE...). */
    @Size(max = 50, message = "Mã lý do tối đa 50 ký tự")
    private String reasonCode;

    @Size(max = 1000, message = "Ghi chú tối đa 1000 ký tự")
    private String reasonNote;
}