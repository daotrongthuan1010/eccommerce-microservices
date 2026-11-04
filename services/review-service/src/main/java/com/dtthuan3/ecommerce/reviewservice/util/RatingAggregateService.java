package com.dtthuan3.ecommerce.reviewservice.Util;

import com.dtthuan3.ecommerce.reviewservice.Util.OutboxWriter;
import com.dtthuan3.ecommerce.reviewservice.entity.RatingAggregate;
import com.dtthuan3.ecommerce.reviewservice.repository.RatingAggregateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;


@Component
@RequiredArgsConstructor
public class RatingAggregateService {

    private final RatingAggregateRepository aggregateRepository;
    private final OutboxWriter outbox;

    public RatingAggregate change(String productId, Integer removedRating, Integer addedRating, String reason) {
        LocalDateTime now = LocalDateTime.now();
        aggregateRepository.insertEmptyIfAbsent(productId, now);
        RatingAggregate aggregate = aggregateRepository.findForUpdate(productId)
                .orElseThrow(() -> new IllegalStateException("Không tạo được rating_aggregate cho " + productId));

        if (removedRating != null && removedRating.equals(addedRating)) {
            return aggregate; // đổi review nhưng giữ nguyên số sao -> điểm tổng không đổi
        }
        if (removedRating == null && addedRating == null) {
            return aggregate;
        }
        if (removedRating != null) {
            aggregate.apply(removedRating, -1);
        }
        if (addedRating != null) {
            aggregate.apply(addedRating, +1);
        }
        aggregate.setAggregateVersion(aggregate.getAggregateVersion() + 1);
        aggregate.setUpdatedAt(now);
        aggregate = aggregateRepository.saveAndFlush(aggregate);
        outbox.enqueueRatingChanged(aggregate, reason);
        return aggregate;
    }
}
