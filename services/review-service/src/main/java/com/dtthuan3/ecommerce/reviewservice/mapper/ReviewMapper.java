package com.dtthuan3.ecommerce.reviewservice.mapper;

import com.dtthuan3.ecommerce.reviewservice.Until.ReviewRules;
import com.dtthuan3.ecommerce.reviewservice.client.OrderLineClient;
import com.dtthuan3.ecommerce.reviewservice.dto.request.ReviewCreateRequest;
import com.dtthuan3.ecommerce.reviewservice.dto.request.ReviewMediaRequest;
import com.dtthuan3.ecommerce.reviewservice.dto.response.AbuseReportResponse;
import com.dtthuan3.ecommerce.reviewservice.dto.response.ReviewMediaResponse;
import com.dtthuan3.ecommerce.reviewservice.dto.response.ReviewResponse;
import com.dtthuan3.ecommerce.reviewservice.entity.*;
import com.dtthuan3.ecommerce.reviewservice.enums.EligibilityStatus;
import com.dtthuan3.ecommerce.reviewservice.enums.ReviewStatus;
import com.dtthuan3.ecommerce.reviewservice.enums.RevisionStatus;
import com.dtthuan3.ecommerce.reviewservice.service.impl.ReviewServiceImpl;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class ReviewMapper {
    /**
     * Review mới luôn ở PENDING_MODERATION, chưa tính vào điểm tổng.
     */
    public Review createReviewEntity(String userId, String userName, EligibilityProjection eligibility,
                                     ReviewCreateRequest request, String content, int mediaCount,
                                     LocalDateTime now) {
        boolean anonymous = request.getIsAnonymous() == null || request.getIsAnonymous();
        return Review.builder()
                .lineId(request.getLineId())
                .orderId(eligibility.getOrderId())
                .userId(userId)
                .productId(eligibility.getProductId())
                .skuId(eligibility.getSkuId())
                .reviewerDisplayName(anonymous ? maskDisplayName(userName) : displayName(userName))
                .anonymous(anonymous)
                .status(ReviewStatus.PENDING_MODERATION)
                .rating(request.getRating())
                .hasPhoto(mediaCount > 0)
                .hasComment(ReviewRules.hasText(content))
                .latestRevisionNo(1)
                .helpfulCount(0)
                .submittedAt(now)
                .editableUntil(now.plusDays(ReviewServiceImpl.EDIT_WINDOW_DAYS))
                .build();
    }

    public String displayName(String rawName) {
        return ReviewRules.hasText(rawName) ? rawName.trim() : "Khách hàng";
    }

    public String maskDisplayName(String rawName) {
        if (!ReviewRules.hasText(rawName)) {
            return "Khách hàng";
        }
        String name = rawName.trim();
        if (name.length() <= 2) {
            return name.charAt(0) + "***";
        }
        return name.charAt(0) + "***" + name.charAt(name.length() - 1);
    }

    public ReviewRevision createRevision(String reviewId, int revisionNo, Integer rating, String content,
                                         LocalDateTime now) {
        return ReviewRevision.builder()
                .reviewId(reviewId)
                .revisionNo(revisionNo)
                .rating(rating)
                .content(content)
                .status(RevisionStatus.PENDING_MODERATION)
                .submittedAt(now)
                .build();
    }

    /**
     * Thứ tự hiển thị = thứ tự trong mảng client gửi.
     */
    public List<ReviewMedia> createMediaList(String reviewId, String revisionId, List<ReviewMediaRequest> requests) {
        List<ReviewMedia> list = new ArrayList<>(requests.size());
        for (int i = 0; i < requests.size(); i++) {
            ReviewMediaRequest r = requests.get(i);
            list.add(ReviewMedia.builder()
                    .reviewId(reviewId)
                    .revisionId(revisionId)
                    .fileId(r.getFileId())
                    .mediaType(r.getMediaType())
                    .sortOrder(i)
                    .build());


        }
        return list;
    }
    public void applyEditToUnpublished(Review review, Integer rating, String content, int mediaCount) {
        review.setStatus(ReviewStatus.PENDING_MODERATION);
        review.setRating(rating);
        review.setHasPhoto(mediaCount > 0);
        review.setHasComment(ReviewRules.hasText(content));
    }
    /** Dòng đơn lấy từ order-service (fallback) -> projection. Event Kafka thật có version >= 1 sẽ ghi đè. */
    public EligibilityProjection toProjection(OrderLineClient.OrderLineInfo line, String lineId,
                                              LocalDateTime deliveredAt) {
        return EligibilityProjection.builder()
                .lineId(lineId)
                .userId(line.userId())
                .orderId(line.orderId())
                .productId(line.productId())
                .skuId(line.skuId())
                .status(EligibilityStatus.ELIGIBLE)
                .deliveredAt(deliveredAt)
                .eligibleUntil(deliveredAt.plusDays(ReviewServiceImpl.ELIGIBILITY_WINDOW_DAYS))
                .sourceEventId(UUID.randomUUID().toString())
                .sourceVersion(0L)
                .build();
    }
    public ReviewResponse.ReviewReplyDto toReplyDto(ReviewReply reply) {
        return ReviewResponse.ReviewReplyDto.builder()
                .id(reply.getId())
                .content(reply.getContent())
                .publishedAt(reply.getCreatedAt())
                .build();
    }
    public ReviewMediaResponse toMediaResponse(ReviewMedia media) {
        return ReviewMediaResponse.builder()
                .fileId(media.getFileId())
                .mediaType(media.getMediaType())
                .mediaUrl(media.getMediaUrl())
                .thumbnailUrl(media.getThumbnailUrl())
                .sortOrder(media.getSortOrder())
                .build();
    }
    public List<ReviewMediaResponse> toMediaResponses(List<ReviewMedia> media) {
        return media == null ? List.of() : media.stream().map(this::toMediaResponse).toList();
    }
    public AbuseReportResponse toReportResponse(AbuseReport report) {
        return AbuseReportResponse.builder()
                .id(report.getId())
                .reviewId(report.getReviewId())
                .reasonCode(report.getReasonCode())
                .note(report.getNote())
                .status(report.getStatus().name())
                .createdAt(report.getCreatedAt())
                .resolvedAt(report.getResolvedAt())
                .build();
    }
}
