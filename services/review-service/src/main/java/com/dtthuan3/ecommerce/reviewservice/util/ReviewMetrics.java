package com.dtthuan3.ecommerce.reviewservice.Util;

import com.dtthuan3.ecommerce.reviewservice.enums.OutboxStatus;
import com.dtthuan3.ecommerce.reviewservice.enums.RevisionStatus;
import com.dtthuan3.ecommerce.reviewservice.repository.OutboxEventRepository;
import com.dtthuan3.ecommerce.reviewservice.repository.ReviewRevisionRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;


@Component
public class ReviewMetrics {

    private final MeterRegistry registry;
    private final Timer projectionLag;

    public ReviewMetrics(MeterRegistry registry, ReviewRevisionRepository revisions, OutboxEventRepository outbox) {
        this.registry = registry;

        Gauge.builder("review.moderation.pending", revisions,
                        r -> r.countByStatus(RevisionStatus.PENDING_MODERATION))
                .description("Số revision đang chờ kiểm duyệt")
                .register(registry);
        Gauge.builder("review.moderation.pending.oldest.age.seconds", revisions,
                        r -> r.findFirstByStatusOrderBySubmittedAtAsc(RevisionStatus.PENDING_MODERATION)
                                .map(x -> Duration.between(x.getSubmittedAt(), LocalDateTime.now()).toSeconds())
                                .orElse(0L))
                .description("Tuổi (giây) của revision chờ duyệt cũ nhất")
                .register(registry);

        Gauge.builder("review.outbox.pending", outbox, o -> o.countByStatus(OutboxStatus.PENDING))
                .description("Số outbox event chưa gửi")
                .register(registry);
        Gauge.builder("review.outbox.failed", outbox, o -> o.countByStatus(OutboxStatus.FAILED))
                .description("Số outbox event FAILED cần xử lý thủ công")
                .register(registry);
        Gauge.builder("review.outbox.oldest.age.seconds", outbox,
                        o -> o.findFirstByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING)
                                .map(x -> Duration.between(x.getCreatedAt(), LocalDateTime.now()).toSeconds())
                                .orElse(0L))
                .description("Relay lag: tuổi (giây) của outbox event PENDING cũ nhất")
                .register(registry);

        this.projectionLag = Timer.builder("review.projection.lag")
                .description("Độ trễ từ lúc giao hàng đến khi có EligibilityProjection")
                .register(registry);
    }

    public void submitBlocked(String reason) {
        registry.counter("review.submit.blocked", "reason", reason).increment();
    }

    public void aggregateMismatch() {
        registry.counter("review.aggregate.mismatch").increment();
    }

    public void inboxDuplicate() {
        registry.counter("review.inbox.duplicate").increment();
    }

    public void projectionLag(Duration lag) {
        if (lag != null && !lag.isNegative()) {
            projectionLag.record(lag);
        }
    }
}
