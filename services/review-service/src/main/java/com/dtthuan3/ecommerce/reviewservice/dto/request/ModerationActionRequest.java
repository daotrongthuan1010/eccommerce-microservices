package com.dtthuan3.ecommerce.reviewservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/** Ẩn / khôi phục review: bắt buộc có lý do chuẩn hóa; có thể gắn với vụ việc báo cáo. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModerationActionRequest {
    @NotBlank(message = "Mã lý do không được để trống")
    @Size(max = 50, message = "Mã lý do tối đa 50 ký tự")
    private String reasonCode;

    @Size(max = 1000, message = "Ghi chú tối đa 1000 ký tự")
    private String reasonNote;

    /** Vụ việc báo cáo dẫn tới quyết định (nếu có) - sẽ được đóng RESOLVED. */
    @Size(max = 36, message = "abuseReportId tối đa 36 ký tự")
    private String abuseReportId;
}
