package com.dtthuan3.ecommerce.reviewservice.service;

import com.dtthuan3.ecommerce.common.response.PageResponse;
import com.dtthuan3.ecommerce.reviewservice.dto.request.ModerationActionRequest;
import com.dtthuan3.ecommerce.reviewservice.dto.request.ModerationRequest;
import com.dtthuan3.ecommerce.reviewservice.dto.request.SellerReplyRequest;
import com.dtthuan3.ecommerce.reviewservice.dto.response.AbuseReportResponse;
import com.dtthuan3.ecommerce.reviewservice.dto.response.ReconcileResponse;
import com.dtthuan3.ecommerce.reviewservice.dto.response.ReviewResponse;

public interface ReviewModerationService {
    /** Hàng chờ duyệt: các REVISION đang PENDING_MODERATION (cũ nhất trước). */
    PageResponse<ReviewResponse> getPendingQueue(int page, int size);

    /** Duyệt / từ chối một revision (REV-05). */
    void decideRevision(String moderatorId, String revisionId, ModerationRequest request);

    /** APPROVED -> HIDDEN, trừ khỏi điểm tổng (REV-UC02). */
    void hideReview(String moderatorId, String reviewId, ModerationActionRequest request);

    /** HIDDEN -> APPROVED, cộng lại vào điểm tổng. */
    void restoreReview(String moderatorId, String reviewId, ModerationActionRequest request);

    PageResponse<AbuseReportResponse> getReports(String status, int page, int size);

    /** Bác bỏ báo cáo (không vi phạm). */
    void dismissReport(String moderatorId, String reportId);

    /** Nhân viên phản hồi đại diện doanh nghiệp; không đụng tới nội dung/sao của khách (REV-06). */
    ReviewResponse.ReviewReplyDto replyToReview(String staffId, String reviewId, SellerReplyRequest request);

    void removeReply(String staffId, String reviewId, String replyId);

    ReconcileResponse reconcile(String productId, boolean repair);
}