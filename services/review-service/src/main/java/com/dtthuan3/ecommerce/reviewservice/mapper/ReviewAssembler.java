package com.dtthuan3.ecommerce.reviewservice.mapper;

import com.dtthuan3.ecommerce.reviewservice.dto.response.ReviewListResponse;
import com.dtthuan3.ecommerce.reviewservice.dto.response.ReviewResponse;
import com.dtthuan3.ecommerce.reviewservice.entity.*;
import com.dtthuan3.ecommerce.reviewservice.enums.ReplyStatus;
import com.dtthuan3.ecommerce.reviewservice.enums.ReviewStatus;
import com.dtthuan3.ecommerce.reviewservice.enums.RevisionStatus;
import com.dtthuan3.ecommerce.reviewservice.repository.*;
import com.dtthuan3.ecommerce.reviewservice.service.impl.ReviewServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Nạp revision/media/reply/like liên quan rồi dựng response (tránh N+1 cho danh sách). */
@Component
@RequiredArgsConstructor
public class ReviewAssembler {

    private final ReviewRevisionRepository revisionRepository;
    private final ReviewMediaRepository mediaRepository;
    private final ReviewReplyRepository replyRepository;
    private final HelpfulVoteRepository voteRepository;
    private final ModerationDecisionRepository decisionRepository;
    private final ReviewMapper mapper;

    /** Chủ review xem: revision MỚI NHẤT (kể cả đang chờ duyệt / bị từ chối, kèm lý do). */
    public ReviewResponse ownerView(Review review) {
        ReviewRevision revision = revisionRepository
                .findByReviewIdAndRevisionNo(review.getId(), review.getLatestRevisionNo())
                .orElse(null);
        return build(review, revision, true);
    }

    /** Người khác xem: revision ĐÃ CÔNG KHAI (APPROVED). */
    public ReviewResponse publicView(Review review) {
        ReviewRevision revision = review.getPublishedRevisionId() == null
                ? null
                : revisionRepository.findById(review.getPublishedRevisionId()).orElse(null);
        return build(review, revision, false);
    }

    /** Kiểm duyệt viên xem đúng revision cần ra quyết định. */
    public ReviewResponse moderationView(Review review, ReviewRevision revision) {
        return build(review, revision, true);
    }

    private ReviewResponse build(Review review, ReviewRevision revision, boolean internalContext) {
        List<ReviewMedia> media = revision == null
                ? List.of()
                : mediaRepository.findByRevisionIdOrderBySortOrderAsc(revision.getId());
        List<ReviewResponse.ReviewReplyDto> replies = replyRepository
                .findByReviewIdInAndStatusOrderByCreatedAtAsc(List.of(review.getId()), ReplyStatus.PUBLISHED)
                .stream().map(mapper::toReplyDto).toList();

        ReviewResponse.ReviewResponseBuilder b = ReviewResponse.builder()
                .id(review.getId())
                .productId(review.getProductId())
                .revisionId(internalContext && revision != null ? revision.getId() : null)
                .rating(revision != null ? revision.getRating() : review.getRating())
                .content(revision != null ? revision.getContent() : null)
                .media(mapper.toMediaResponses(media))
                .reviewerDisplayName(review.getReviewerDisplayName())
                .anonymous(Boolean.TRUE.equals(review.getAnonymous()))
                .verifiedPurchase(true)
                .status(review.getStatus() != null ? review.getStatus().name() : null)
                .revisionStatus(revision != null ? revision.getStatus().name() : null)
                .revisionNo(revision != null ? revision.getRevisionNo() : review.getLatestRevisionNo())
                .helpfulCount(review.getHelpfulCount())
                .replies(replies)
                .submittedAt(review.getSubmittedAt())
                .publishedAt(review.getPublishedAt())
                .version(review.getVersion());

        if (internalContext) {
            b.lineId(review.getLineId())
                    .editableUntil(review.getEditableUntil())
                    .canEdit(canEdit(review))
                    .pendingRevision(revision != null && revision.getStatus() == RevisionStatus.PENDING_MODERATION);
            if (revision != null && revision.getStatus() == RevisionStatus.REJECTED) {
                decisionRepository.findFirstByRevisionIdOrderByDecidedAtDesc(revision.getId())
                        .ifPresent(d -> applyRejection(b, d));
            }
        }
        return b.build();
    }

    private void applyRejection(ReviewResponse.ReviewResponseBuilder b, ModerationDecision decision) {
        b.rejectionReasonCode(decision.getReasonCode()).rejectionReasonNote(decision.getReasonNote());
    }

    /** Còn sửa được không: chưa xóa/ẩn, còn hạn, chưa quá số lần sửa. */
    public boolean canEdit(Review review) {
        if (review.getStatus() == ReviewStatus.DELETED || review.getStatus() == ReviewStatus.HIDDEN) {
            return false;
        }
        boolean inWindow = review.getEditableUntil() != null
                && review.getEditableUntil().isAfter(LocalDateTime.now());
        return inWindow && ReviewServiceImpl.canEditMore(review.getLatestRevisionNo() - 1);
    }

    /** Danh sách công khai: nạp theo lô, không truy vấn lặp từng review. */
    public List<ReviewListResponse> listItems(List<Review> reviews, String viewerId) {
        if (reviews.isEmpty()) {
            return List.of();
        }
        List<String> reviewIds = reviews.stream().map(Review::getId).toList();
        Set<String> revisionIds = reviews.stream()
                .map(Review::getPublishedRevisionId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<String, ReviewRevision> revisions = revisionIds.isEmpty()
                ? Map.of()
                : revisionRepository.findAllById(revisionIds).stream()
                .collect(Collectors.toMap(ReviewRevision::getId, Function.identity()));
        Map<String, List<ReviewMedia>> mediaByRevision = revisionIds.isEmpty()
                ? Map.of()
                : mediaRepository.findByRevisionIdInOrderBySortOrderAsc(revisionIds).stream()
                .collect(Collectors.groupingBy(ReviewMedia::getRevisionId));

        Map<String, List<ReviewResponse.ReviewReplyDto>> repliesByReview = new java.util.HashMap<>();
        for (ReviewReply reply : replyRepository
                .findByReviewIdInAndStatusOrderByCreatedAtAsc(reviewIds, ReplyStatus.PUBLISHED)) {
            repliesByReview.computeIfAbsent(reply.getReviewId(), k -> new ArrayList<>())
                    .add(mapper.toReplyDto(reply));
        }

        Set<String> liked = viewerId == null
                ? Set.of()
                : new HashSet<>(voteRepository.findLikedReviewIds(viewerId, reviewIds));

        return reviews.stream().map(r -> {
            ReviewRevision rev = r.getPublishedRevisionId() == null ? null : revisions.get(r.getPublishedRevisionId());
            return ReviewListResponse.builder()
                    .id(r.getId())
                    .displayUserName(r.getReviewerDisplayName())
                    .rating(r.getRating())
                    .comment(rev != null ? rev.getContent() : null)
                    .verifiedPurchase(true)
                    .anonymous(Boolean.TRUE.equals(r.getAnonymous()))
                    .edited(rev != null && rev.getRevisionNo() != null && rev.getRevisionNo() > 1)
                    .helpfulCount(r.getHelpfulCount() == null ? 0 : r.getHelpfulCount())
                    .likedByMe(viewerId == null ? null : liked.contains(r.getId()))
                    .replies(repliesByReview.getOrDefault(r.getId(), List.of()))
                    .media(mapper.toMediaResponses(
                            rev != null ? mediaByRevision.getOrDefault(rev.getId(), List.of()) : List.of()))
                    .createdAt(r.getSubmittedAt())
                    .publishedAt(r.getPublishedAt())
                    .build();
        }).toList();
    }
}
