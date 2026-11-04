package com.dtthuan3.ecommerce.reviewservice.util;

import com.dtthuan3.ecommerce.reviewservice.client.FileVerificationClient;
import com.dtthuan3.ecommerce.reviewservice.entity.Review;
import com.dtthuan3.ecommerce.reviewservice.entity.ReviewMedia;
import com.dtthuan3.ecommerce.reviewservice.entity.ReviewRevision;
import com.dtthuan3.ecommerce.reviewservice.enums.ReviewStatus;
import com.dtthuan3.ecommerce.reviewservice.enums.RevisionStatus;
import com.dtthuan3.ecommerce.reviewservice.repository.ReviewMediaRepository;
import com.dtthuan3.ecommerce.reviewservice.repository.ReviewRepository;
import com.dtthuan3.ecommerce.reviewservice.repository.ReviewRevisionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Một nơi duy nhất để "duyệt một revision và đưa lên công khai" (dùng cho tự động duyệt lẫn kiểm duyệt viên).
 * Phải gọi trong transaction đang mở, review đã được khóa (findForUpdate).
 * Thứ tự: kiểm/ghi binding ảnh -> đổi trạng thái -> phát ReviewApproved -> cập nhật rating_aggregate
 * (chỉ phần chênh lệch nên không đếm hai revision của cùng một review).
 */
@Component
@RequiredArgsConstructor
public class ReviewPublisher {

    private final ReviewRepository reviewRepository;
    private final ReviewRevisionRepository revisionRepository;
    private final ReviewMediaRepository mediaRepository;
    private final FileVerificationClient fileClient;
    private final RatingAggregateService aggregateService;
    private final OutboxWriter outbox;

    public void approve(Review review, ReviewRevision revision, List<ReviewMedia> media) {
        LocalDateTime now = LocalDateTime.now();

        // File chưa READY thì không được công khai; binding tạo ngay trước khi công khai
        List<String> fileIds = media.stream().map(ReviewMedia::getFileId).toList();
        fileClient.ensureReady(review.getUserId(), fileIds);
        Map<String, String> bindings = fileClient.bind(revision.getId(), fileIds);
        if (!bindings.isEmpty()) {
            media.forEach(m -> {
                String bindingId = bindings.get(m.getFileId());
                if (bindingId != null) {
                    m.setBindingId(bindingId);
                }
            });
            mediaRepository.saveAll(media);
        }

        // Review này đang được tính trong điểm tổng với số sao nào? (chỉ khi đang APPROVED)
        boolean counted = review.getStatus() == ReviewStatus.APPROVED && review.getPublishedRevisionId() != null;
        Integer previousRating = counted ? review.getRating() : null;

        String oldPublishedId = review.getPublishedRevisionId();
        if (oldPublishedId != null && !oldPublishedId.equals(revision.getId())) {
            revisionRepository.findById(oldPublishedId).ifPresent(old -> {
                if (old.getStatus() == RevisionStatus.APPROVED) {
                    old.setStatus(RevisionStatus.SUPERSEDED);
                }
            });
        }

        revision.setStatus(RevisionStatus.APPROVED);
        revision.setModeratedAt(now);
        revisionRepository.save(revision);

        review.setStatus(ReviewStatus.APPROVED);
        review.setPublishedRevisionId(revision.getId());
        review.setRating(revision.getRating());
        review.setHasPhoto(!media.isEmpty());
        review.setHasComment(ReviewRules.hasText(revision.getContent()));
        review.setPublishedAt(now); // "thời điểm duyệt gần nhất" - khóa sắp xếp mặc định
        reviewRepository.saveAndFlush(review);

        Map<String, Object> extra = new LinkedHashMap<>();
        extra.put("rating", revision.getRating());
        extra.put("previousRating", previousRating);
        extra.put("revisionNo", revision.getRevisionNo());
        extra.put("hasMedia", !media.isEmpty());
        outbox.enqueue(OutboxWriter.EVENT_APPROVED, review, extra);

        aggregateService.change(review.getProductId(), previousRating, revision.getRating(), "REVIEW_APPROVED");
    }
}
