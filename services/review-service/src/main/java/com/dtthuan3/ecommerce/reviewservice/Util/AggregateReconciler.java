package com.dtthuan3.ecommerce.reviewservice.Util;

import com.dtthuan3.ecommerce.reviewservice.dto.response.ReconcileResponse;
import com.dtthuan3.ecommerce.reviewservice.entity.RatingAggregate;
import com.dtthuan3.ecommerce.reviewservice.enums.ReviewStatus;
import com.dtthuan3.ecommerce.reviewservice.repository.RatingAggregateRepository;
import com.dtthuan3.ecommerce.reviewservice.repository.ReviewRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Đối chiếu rating_aggregate với tập review APPROVED (mục 6.3 / 9.2) và sửa nếu lệch.
 * Rebuild chỉ ghi lại aggregate + phát ReviewRatingChanged với version mới; KHÔNG tạo review hay notification.
 * Khóa phân tán giữa các instance: pg_try_advisory_xact_lock (mục 11.6 cho phép chọn @Scheduled + khóa).
 */
@Slf4j
@Component
public class AggregateReconciler {

    private static final int PAGE_SIZE = 500;
    private static final long ADVISORY_LOCK_KEY = 7_204_001L;

    private final ReviewRepository reviewRepository;
    private final RatingAggregateRepository aggregateRepository;
    private final OutboxWriter outbox;
    private final ReviewMetrics metrics;
    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;

    public AggregateReconciler(ReviewRepository reviewRepository, RatingAggregateRepository aggregateRepository,
                               OutboxWriter outbox, ReviewMetrics metrics,
                               JdbcTemplate jdbc, PlatformTransactionManager txManager) {
        this.reviewRepository = reviewRepository;
        this.aggregateRepository = aggregateRepository;
        this.outbox = outbox;
        this.metrics = metrics;
        this.jdbc = jdbc;
        this.tx = new TransactionTemplate(txManager);
    }

    /** Đối chiếu một sản phẩm; {@code repair=true} thì sửa aggregate theo tập APPROVED. */
    @Transactional
    public ReconcileResponse reconcileProduct(String productId, boolean repair) {
        LocalDateTime now = LocalDateTime.now();
        aggregateRepository.insertEmptyIfAbsent(productId, now);
        RatingAggregate aggregate = aggregateRepository.findForUpdate(productId)
                .orElseThrow(() -> new IllegalStateException("Không tạo được rating_aggregate cho " + productId));

        long[] expected = new long[6];
        long expectedTotal = 0;
        int expectedCount = 0;
        for (Object[] row : reviewRepository.countByRating(productId, ReviewStatus.APPROVED)) {
            if (row[0] == null) {
                continue;
            }
            int star = ((Number) row[0]).intValue();
            long count = ((Number) row[1]).longValue();
            if (star >= 1 && star <= 5) {
                expected[star] = count;
                expectedCount += (int) count;
                expectedTotal += star * count;
            }
        }

        boolean consistent = aggregate.getTotalStars() == expectedTotal && aggregate.getReviewCount() == expectedCount;
        for (int star = 1; star <= 5 && consistent; star++) {
            consistent = aggregate.star(star) == expected[star];
        }

        ReconcileResponse.ReconcileResponseBuilder result = ReconcileResponse.builder()
                .productId(productId)
                .consistent(consistent)
                .actualTotalStars(aggregate.getTotalStars())
                .actualReviewCount(aggregate.getReviewCount())
                .expectedTotalStars(expectedTotal)
                .expectedReviewCount(expectedCount);

        boolean repaired = false;
        if (!consistent) {
            metrics.aggregateMismatch();
            log.warn("rating_aggregate của {} lệch: actual={}/{} expected={}/{}", productId,
                    aggregate.getTotalStars(), aggregate.getReviewCount(), expectedTotal, expectedCount);
            if (repair) {
                aggregate.setTotalStars(expectedTotal);
                aggregate.setReviewCount(expectedCount);
                for (int star = 1; star <= 5; star++) {
                    aggregate.setStar(star, (int) expected[star]);
                }
                aggregate.setAggregateVersion(aggregate.getAggregateVersion() + 1);
                aggregate.setUpdatedAt(now);
                repaired = true;
            }
        }
        aggregate.setLastReconciledAt(now);
        aggregate = aggregateRepository.saveAndFlush(aggregate);
        if (repaired) {
            outbox.enqueueRatingChanged(aggregate, "RECONCILE");
        }
        return result.repaired(repaired).build();
    }

    /** Đối chiếu toàn bộ sản phẩm theo từng trang (mỗi trang một transaction, chỉ một instance chạy). */
    public void reconcileAll() {
        int pageSize = PAGE_SIZE;
        String cursor = "";
        while (true) {
            final String after = cursor;
            List<String> page = tx.execute(status -> {
                Boolean locked = jdbc.queryForObject("select pg_try_advisory_xact_lock(?)", Boolean.class,
                        ADVISORY_LOCK_KEY);
                if (!Boolean.TRUE.equals(locked)) {
                    return null;
                }
                List<String> ids = jdbc.queryForList(
                        "select product_id from (select product_id from rating_aggregate "
                                + "union select product_id from review where status = 'APPROVED') t "
                                + "where product_id > ? order by product_id limit ?",
                        String.class, after, pageSize);
                ids.forEach(id -> reconcileProduct(id, true));
                return ids;
            });
            if (page == null) {
                log.info("Một instance khác đang đối chiếu aggregate, bỏ qua lượt này");
                return;
            }
            if (page.size() < pageSize) {
                return;
            }
            cursor = page.get(page.size() - 1);
        }
    }
}
