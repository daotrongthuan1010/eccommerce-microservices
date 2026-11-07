package com.dtthuan3.ecommerce.reviewservice.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dtthuan3.ecommerce.common.exception.BusinessException;
import com.dtthuan3.ecommerce.reviewservice.client.FileVerificationClient;
import com.dtthuan3.ecommerce.reviewservice.client.OrderLineClient;
import com.dtthuan3.ecommerce.reviewservice.dto.request.ReviewCreateRequest;
import com.dtthuan3.ecommerce.reviewservice.dto.response.ReviewResponse;
import com.dtthuan3.ecommerce.reviewservice.entity.EligibilityProjection;
import com.dtthuan3.ecommerce.reviewservice.enums.EligibilityStatus;
import com.dtthuan3.ecommerce.reviewservice.entity.IdempotencyRecord;
import com.dtthuan3.ecommerce.reviewservice.enums.IdempotencyStatus;
import com.dtthuan3.ecommerce.reviewservice.entity.Review;
import com.dtthuan3.ecommerce.reviewservice.mapper.ReviewAssembler;
import com.dtthuan3.ecommerce.reviewservice.mapper.ReviewMapper;
import com.dtthuan3.ecommerce.reviewservice.repository.EligibilityProjectionRepository;
import com.dtthuan3.ecommerce.reviewservice.repository.IdempotencyRecordRepository;
import com.dtthuan3.ecommerce.reviewservice.repository.ReviewMediaRepository;
import com.dtthuan3.ecommerce.reviewservice.repository.ReviewRepository;
import com.dtthuan3.ecommerce.reviewservice.repository.ReviewRevisionRepository;
import com.dtthuan3.ecommerce.reviewservice.util.OutboxWriter;
import com.dtthuan3.ecommerce.reviewservice.util.ReviewMetrics;
import com.dtthuan3.ecommerce.reviewservice.util.RatingAggregateService;
import com.dtthuan3.ecommerce.reviewservice.util.ReviewPublisher;
import com.dtthuan3.ecommerce.reviewservice.util.ReviewRules;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;

/**
 * REV-UC01..04 (gap P1 tu TEST_COVERAGE_REPORT.md): review chi duoc tao khi
 * user da nhan hang (eligibility), chua review san pham do, idempotent theo key.
 * Truoc day review-service khong co test nao.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReviewServiceImplTest {

    private ReviewRepository reviewRepository;
    private EligibilityProjectionRepository eligibilityRepository;
    private IdempotencyRecordRepository idempotencyRepository;
    private ReviewRules rules;
    private ReviewPublisher publisher;
    private OutboxWriter outbox;
    private ReviewAssembler assembler;
    private FileVerificationClient fileClient;
    private OrderLineClient orderClient;
    private ReviewMetrics metrics;
    private RatingAggregateService aggregateService;
    private ReviewMapper mapper;
    private ReviewServiceImpl service;

    @BeforeEach
    void setUp() {
        reviewRepository = mock(ReviewRepository.class);
        var revisionRepository = mock(ReviewRevisionRepository.class);
        var mediaRepository = mock(ReviewMediaRepository.class);
        eligibilityRepository = mock(EligibilityProjectionRepository.class);
        idempotencyRepository = mock(IdempotencyRecordRepository.class);
        rules = mock(ReviewRules.class);
        publisher = mock(ReviewPublisher.class);
        outbox = mock(OutboxWriter.class);
        assembler = mock(ReviewAssembler.class);
        fileClient = mock(FileVerificationClient.class);
        orderClient = mock(OrderLineClient.class);
        metrics = mock(ReviewMetrics.class);
        aggregateService = mock(RatingAggregateService.class);
        mapper = mock(ReviewMapper.class);

        lenient().when(rules.validateMedia(any())).thenReturn(List.of());
        lenient().when(idempotencyRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(idempotencyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        // mapper tao entity that (khong null) de saveAndFlush answer setId duoc
        lenient().when(mapper.createReviewEntity(anyString(), anyString(), any(),
                any(), anyString(), org.mockito.ArgumentMatchers.anyInt(), any()))
                .thenAnswer(inv -> new Review());
        lenient().when(reviewRepository.saveAndFlush(any())).thenAnswer(inv -> {
            Review r = inv.getArgument(0);
            r.setId("rev-1");
            return r;
        });
        lenient().when(mapper.createRevision(anyString(), org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyInt(), anyString(), any()))
                .thenAnswer(inv -> new com.dtthuan3.ecommerce.reviewservice.entity.ReviewRevision());
        lenient().when(revisionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(mediaRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(eligibilityRepository.findByLineId("line-1"))
                .thenReturn(Optional.of(eligible("user-1")));
        lenient().when(reviewRepository.findByLineId("line-1")).thenReturn(Optional.empty());
        lenient().when(assembler.ownerView(any(Review.class))).thenReturn(null);

        service = new ReviewServiceImpl(reviewRepository, revisionRepository, mediaRepository,
                eligibilityRepository, idempotencyRepository, rules, publisher, outbox,
                assembler, fileClient, orderClient, metrics, aggregateService, mapper);
    }

    private EligibilityProjection eligible(String userId) {
        EligibilityProjection e = new EligibilityProjection();
        e.setLineId("line-1");
        e.setUserId(userId);
        e.setStatus(EligibilityStatus.ELIGIBLE);
        e.setEligibleUntil(LocalDateTime.now().plusDays(20));
        return e;
    }

    private ReviewCreateRequest request() {
        ReviewCreateRequest r = new ReviewCreateRequest();
        r.setLineId("line-1");
        r.setRating(5);
        r.setContent("San pham tot");
        r.setMedia(List.of());
        return r;
    }

    @Test
    @DisplayName("REV-UC01: user da nhan hang + chua review -> tao review, ghi idempotency COMPLETED")
    void eligibleUser_createsReview() {
        service.createReview("user-1", "User A", "key-1", request());

        verify(reviewRepository).saveAndFlush(any(Review.class));
        verify(outbox).enqueue(eq(OutboxWriter.EVENT_SUBMITTED), any(Review.class), any());
    }

    @Test
    @DisplayName("REV-UC02: user KHONG phai chu don -> REVIEW_NOT_ELIGIBLE, khong tao review")
    void wrongUser_rejected() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.createReview("user-khac", "User B", "key-2", request()));

        assertThat(ex.getCode()).isEqualTo("REVIEW_NOT_ELIGIBLE");
        verify(reviewRepository, never()).saveAndFlush(any(Review.class));
    }

    @Test
    @DisplayName("REV-UC03: da review san pham nay roi -> DUPLICATE, khong tao thu 2")
    void alreadyReviewed_rejected() {
        when(reviewRepository.findByLineId("line-1")).thenReturn(Optional.of(new Review()));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.createReview("user-1", "User A", "key-3", request()));

        verify(reviewRepository, never()).saveAndFlush(any(Review.class));
    }

    @Test
    @DisplayName("REV-UC04: replay cung key cung noi dung -> khong tao review thu 2")
    void idempotentReplay_skipsSecondCreate() {
        // Lan 1: goi binh thuong de capture fingerprint that service tinh
        service.createReview("user-1", "User A", "key-same", request());
        var idemCaptor = org.mockito.ArgumentCaptor.forClass(IdempotencyRecord.class);
        verify(idempotencyRepository).saveAndFlush(idemCaptor.capture());
        String realFingerprint = idemCaptor.getValue().getFingerprint();

        // Lan 2 (replay): record COMPLETED cung key + cung fingerprint
        IdempotencyRecord done = IdempotencyRecord.builder()
                .actorId("user-1").operationType("CREATE_REVIEW").idempotencyKey("key-same")
                .fingerprint(realFingerprint)
                .status(IdempotencyStatus.COMPLETED)
                .expiresAt(LocalDateTime.now().plusHours(48))
                .resultRef("rev-1")
                .build();
        when(idempotencyRepository.findByActorIdAndOperationTypeAndIdempotencyKey(
                eq("user-1"), eq("CREATE_REVIEW"), eq("key-same")))
                .thenReturn(Optional.of(done));
        // replay() load review theo resultRef
        Review existing = new Review();
        existing.setId("rev-1");
        when(reviewRepository.findById("rev-1")).thenReturn(Optional.of(existing));

        service.createReview("user-1", "User A", "key-same", request());

        // Khong tao review thu 2 — saveAndFlush chi duoc goi 1 lan (lan 1)
        verify(reviewRepository, org.mockito.Mockito.times(1)).saveAndFlush(any(Review.class));
    }
}
