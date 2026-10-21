package com.dtthuan3.ecommerce.reviewservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL) // Tự động loại bỏ các trường null khi parse ra JSON
public class ReviewResponse {

    // --- Thông tin định danh ---
    private String id;
    private String productId;

    // --- Nội dung đánh giá (Lấy từ Revision & Image) ---
    private Integer rating;
    private String content;
    private List<String> images; // Danh sách các fileId hoặc URL ảnh

    // --- Thông tin hiển thị ---
    private String reviewerDisplayName;
    private Boolean verifiedPurchase; // Cờ đánh dấu "Đã mua hàng"

    // --- Trạng thái & Tương tác ---
    private String status;
    private Integer revisionNo;
    private Integer helpfulCount;
    private ReviewReplyDto reply; // Phản hồi của doanh nghiệp

    // --- Thời gian ---
    private LocalDateTime submittedAt;
    private LocalDateTime publishedAt;
    private LocalDateTime editableUntil;

    // --- Version để kiểm soát Update ---
    private Long version;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReviewReplyDto {
        private String content;
        private LocalDateTime publishedAt;
    }
}
