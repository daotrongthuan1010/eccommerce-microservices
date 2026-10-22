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
    private String id;
    private String productId;
    /** Chỉ trả cho chủ review / kiểm duyệt viên. */
    private String lineId;
    /** Revision đang xem (kiểm duyệt viên dùng để ra quyết định). */
    private String revisionId;

    private Integer rating;
    private String content;
    private List<ReviewMediaResponse> media;

    private String reviewerDisplayName;
    private Boolean anonymous;
    /** Nhãn "đã mua". */
    private Boolean verifiedPurchase;

    private String status;
    /** Trạng thái của revision đang xem (PENDING_MODERATION/APPROVED/REJECTED/SUPERSEDED). */
    private String revisionStatus;
    private Integer revisionNo;
    private Integer helpfulCount;
    private List<ReviewReplyDto> replies;

    /** Lý do bị từ chối - chỉ chủ review và kiểm duyệt viên thấy. */
    private String rejectionReasonCode;
    private String rejectionReasonNote;

    /** Còn sửa được không (còn hạn, chưa quá số lần). Chỉ trả cho chủ review. */
    private Boolean canEdit;
    /** Có bản sửa đang chờ duyệt không. Chỉ trả cho chủ review. */
    private Boolean pendingRevision;

    private LocalDateTime submittedAt;
    private LocalDateTime publishedAt;
    private LocalDateTime editableUntil;

    /** Version để gửi lại khi cập nhật (optimistic locking). */
    private Long version;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReviewReplyDto {
        private String id;
        private String content;
        private LocalDateTime publishedAt;
    }
}