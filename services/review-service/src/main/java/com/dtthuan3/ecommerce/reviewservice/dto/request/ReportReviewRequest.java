package com.dtthuan3.ecommerce.reviewservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportReviewRequest {

    /** VD: SPAM, OFFENSIVE, IRRELEVANT, PERSONAL_INFO. */
    @NotBlank(message = "Lý do báo cáo không được để trống")
    @Size(max = 50, message = "Lý do tối đa 50 ký tự")
    private String reasonCode;

    @Size(max = 1000, message = "Ghi chú tối đa 1000 ký tự")
    private String note;
}
