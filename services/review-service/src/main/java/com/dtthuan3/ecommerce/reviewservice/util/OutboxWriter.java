package com.dtthuan3.ecommerce.reviewservice.util;

import com.dtthuan3.ecommerce.reviewservice.entity.OutboxEvent;
import com.dtthuan3.ecommerce.reviewservice.entity.RatingAggregate;
import com.dtthuan3.ecommerce.reviewservice.entity.Review;
import com.dtthuan3.ecommerce.reviewservice.enums.OutboxStatus;
import com.dtthuan3.ecommerce.reviewservice.repository.OutboxEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Ghi event vào bảng outbox CÙNG transaction với thay đổi nghiệp vụ (Transactional Outbox).
 * Phải gọi sau khi entity đã flush để aggregateVersion phản ánh đúng version hiện tại.
 * Mọi payload đều có productId: relay dùng làm khóa phân vùng Kafka (giữ thứ tự theo sản phẩm).
 */
@Component
@RequiredArgsConstructor
public class OutboxWriter {

    public static final String EVENT_SUBMITTED = "ReviewSubmitted";
    public static final String EVENT_APPROVED = "ReviewApproved";
    public static final String EVENT_REJECTED = "ReviewRejected";
    public static final String EVENT_HIDDEN = "ReviewHidden";
    public static final String EVENT_RESTORED = "ReviewRestored";
    public static final String EVENT_DELETED = "ReviewDeleted";
    public static final String EVENT_RATING_CHANGED = "ReviewRatingChanged";

    private final OutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;

    /** Event của một review (aggregate = REVIEW). */
    public void enqueue(String eventType, Review review, Map<String, Object> extra) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("reviewId", review.getId());
        payload.put("productId", review.getProductId());
        payload.put("userId", review.getUserId());
        if (extra != null) {
            payload.putAll(extra);
        }
        save(eventType, "REVIEW", review.getId(), review.getVersion() == null ? 0L : review.getVersion(), payload);
    }

    /** ReviewRatingChanged: snapshot aggregate kèm aggregateVersion (Catalog bỏ version cũ hơn). */
    public void enqueueRatingChanged(RatingAggregate aggregate, String reason) {
        Map<String, Object> distribution = new LinkedHashMap<>();
        for (int star = 5; star >= 1; star--) {
            distribution.put(String.valueOf(star), aggregate.star(star));
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("productId", aggregate.getProductId());
        payload.put("totalStars", aggregate.getTotalStars());
        payload.put("reviewCount", aggregate.getReviewCount());
        payload.put("average", aggregate.getReviewCount() == 0 ? 0.0
                : Math.round(aggregate.average() * 100.0) / 100.0);
        payload.put("distribution", distribution);
        payload.put("sourceVersion", aggregate.getAggregateVersion());
        payload.put("reason", reason);
        save(EVENT_RATING_CHANGED, "RATING_AGGREGATE", aggregate.getProductId(),
                aggregate.getAggregateVersion(), payload);
    }

    private void save(String eventType, String aggregateType, String aggregateId, long aggregateVersion,
                      Map<String, Object> payload) {
        String eventId = UUID.randomUUID().toString();
        String traceId = MDC.get("traceId");
        try {
            outboxRepository.save(OutboxEvent.builder()
                    .eventId(eventId)
                    .aggregateType(aggregateType)
                    .aggregateId(aggregateId)
                    .aggregateVersion(aggregateVersion)
                    .eventType(eventType)
                    .schemaVersion(1)
                    .payload(objectMapper.writeValueAsString(payload))
                    .status(OutboxStatus.PENDING)
                    .correlationId(traceId != null ? traceId : eventId)
                    .retryCount(0)
                    .build());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Không thể serialize payload của event " + eventType, e);
        }
    }
}
