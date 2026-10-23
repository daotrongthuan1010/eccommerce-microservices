package com.dtthuan3.ecommerce.reviewservice.dto.response;

import lombok.*;

import java.time.LocalDateTime;

/** Vụ việc báo cáo cho hàng chờ của kiểm duyệt viên (không lộ danh tính người báo cáo). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AbuseReportResponse {
    private String id;
    private String reviewId;
    private String reasonCode;
    private String note;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;
}
