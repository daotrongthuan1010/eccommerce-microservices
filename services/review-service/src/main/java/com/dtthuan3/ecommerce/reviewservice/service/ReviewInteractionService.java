package com.dtthuan3.ecommerce.reviewservice.service;

import com.dtthuan3.ecommerce.reviewservice.dto.request.ReportReviewRequest;
import com.dtthuan3.ecommerce.reviewservice.dto.response.HelpfulResponse;

public interface ReviewInteractionService {
    /** Đặt trạng thái "hữu ích" mong muốn (true/false); gửi lặp cùng giá trị cho cùng kết quả (REV-07). */
    HelpfulResponse setHelpful(String userId, String reviewId, boolean helpful);

    /** Báo cáo đánh giá vi phạm (mỗi người một lần cho mỗi review); KHÔNG tự động ẩn review. */
    void report(String userId, String reviewId, ReportReviewRequest request);
}
