
package com.dtthuan3.ecommerce.productservice.service.impl;

import com.dtthuan3.ecommerce.common.exception.BusinessException;
import com.dtthuan3.ecommerce.common.exception.ErrorCode;
import com.dtthuan3.ecommerce.common.exception.ResourceNotFoundException;
import com.dtthuan3.ecommerce.productservice.constant.OutboxStatus;
import com.dtthuan3.ecommerce.productservice.constant.ProductStatus;
import com.dtthuan3.ecommerce.productservice.domain.OutboxEvent;
import com.dtthuan3.ecommerce.productservice.domain.Product;
import com.dtthuan3.ecommerce.productservice.domain.ProductPublishingHistory;
import com.dtthuan3.ecommerce.productservice.domain.ProductRevision;
import com.dtthuan3.ecommerce.productservice.domain.PublicationApproval;
import com.dtthuan3.ecommerce.productservice.dto.response.ProductPublishingHistoryResponse;
import com.dtthuan3.ecommerce.productservice.mapper.ProductPublishingHistoryMapper;
import com.dtthuan3.ecommerce.productservice.repository.OutboxEventRepository;
import com.dtthuan3.ecommerce.productservice.repository.ProductPublishingHistoryRepository;
import com.dtthuan3.ecommerce.productservice.repository.ProductRepository;
import com.dtthuan3.ecommerce.productservice.repository.ProductRevisionRepository;
import com.dtthuan3.ecommerce.productservice.repository.PublicationApprovalRepository;
import com.dtthuan3.ecommerce.productservice.service.ProductPublishingService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductPublishingServiceImpl
        implements ProductPublishingService {

    private static final String APPROVAL_PENDING = "PENDING";
    private static final String APPROVAL_APPROVED = "APPROVED";
    private static final String APPROVAL_REJECTED = "REJECTED";

    private static final String REVISION_DRAFT = "DRAFT";
    private static final String REVISION_PENDING_APPROVAL = "PENDING_APPROVAL";
    private static final String REVISION_PUBLISHED = "PUBLISHED";
    private static final String REVISION_REJECTED = "REJECTED";

    private static final String OUTBOX_TOPIC = "product-events";

    private final ProductRepository productRepository;
    private final ProductRevisionRepository productRevisionRepository;
    private final PublicationApprovalRepository publicationApprovalRepository;
    private final ProductPublishingHistoryRepository publishingHistoryRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ProductPublishingHistoryMapper publishingHistoryMapper;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public UUID submitForApproval(
            UUID productId,
            UUID revisionId,
            UUID actorId
    ) {
        requireActor(actorId);

        Product product = productRepository.findById(productId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Product", productId));

        ProductRevision revision =
                productRevisionRepository.findByIdAndProductId(
                                revisionId,
                                productId
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "ProductRevision",
                                        revisionId
                                ));

        if (product.getStatus() == ProductStatus.DISCONTINUED) {
            throw new BusinessException(
                    ErrorCode.CONFLICT,
                    "Không thể gửi duyệt sản phẩm đã ngừng kinh doanh"
            );
        }

        if (!REVISION_DRAFT.equals(revision.getStatus())) {
            throw new BusinessException(
                    ErrorCode.CONFLICT,
                    "Chỉ revision ở trạng thái DRAFT mới được gửi duyệt"
            );
        }

        if (revision.getName() == null || revision.getName().isBlank()) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_ERROR,
                    "Tên sản phẩm không được để trống"
            );
        }

        LocalDateTime now = LocalDateTime.now();
        ProductStatus previousStatus = product.getStatus();

        PublicationApproval approval = PublicationApproval.builder()
                .revisionId(revisionId)
                .submittedBy(actorId)
                .status(APPROVAL_PENDING)
                .submittedAt(now)
                .build();

        publicationApprovalRepository.save(approval);

        revision.setStatus(REVISION_PENDING_APPROVAL);
        revision.setSubmittedAt(now);
        product.setCurrentRevisionId(revisionId);

        // Sản phẩm chưa từng xuất bản: chuyển sang PENDING.
        // Sản phẩm đã ACTIVE: giữ bản xuất bản hiện tại cho đến khi
        // revision mới được duyệt.
        if (product.getPublishedRevisionId() == null) {
            product.setStatus(ProductStatus.PENDING);
        }

        productRepository.save(product);
        productRevisionRepository.save(revision);

        saveHistory(
                productId,
                revisionId,
                previousStatus,
                product.getStatus(),
                "SUBMITTED_FOR_APPROVAL",
                actorId,
                now
        );

        saveOutboxEvent(
                productId,
                revisionId,
                "ProductSubmittedForApproval",
                actorId,
                approval.getId(),
                now
        );

        return approval.getId();
    }

    @Override
    @Transactional
    public void approve(UUID approvalId, UUID actorId) {
        requireActor(actorId);

        PublicationApproval approval = getApproval(approvalId);

        if (!APPROVAL_PENDING.equals(approval.getStatus())) {
            throw new BusinessException(
                    ErrorCode.CONFLICT,
                    "Yêu cầu duyệt không còn ở trạng thái PENDING"
            );
        }

        ProductRevision revision = productRevisionRepository
                .findById(approval.getRevisionId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "ProductRevision",
                        approval.getRevisionId()
                ));

        Product product = productRepository.findById(revision.getProductId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Product",
                                revision.getProductId()
                        ));

        ensureCurrentPendingRevision(product, revision);

        LocalDateTime now = LocalDateTime.now();
        ProductStatus previousStatus = product.getStatus();

        approval.setStatus(APPROVAL_APPROVED);
        approval.setApprovedBy(actorId);
        approval.setApprovedAt(now);

        revision.setStatus(REVISION_PUBLISHED);
        revision.setPublishedAt(now);
        revision.setRejectionReason(null);

        product.setPublishedRevisionId(revision.getId());
        product.setCurrentRevisionId(revision.getId());
        product.setStatus(ProductStatus.ACTIVE);

        publicationApprovalRepository.save(approval);
        productRevisionRepository.save(revision);
        productRepository.save(product);

        saveHistory(
                product.getId(),
                revision.getId(),
                previousStatus,
                ProductStatus.ACTIVE,
                "PUBLICATION_APPROVED",
                actorId,
                now
        );

        saveOutboxEvent(
                product.getId(),
                revision.getId(),
                "ProductPublished",
                actorId,
                approval.getId(),
                now
        );
    }

    @Override
    @Transactional
    public void reject(
            UUID approvalId,
            UUID actorId,
            String reason
    ) {
        requireActor(actorId);

        if (reason == null || reason.isBlank()) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_ERROR,
                    "Lý do từ chối không được để trống"
            );
        }

        if (reason.length() > 1000) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_ERROR,
                    "Lý do từ chối không được vượt quá 1000 ký tự"
            );
        }

        PublicationApproval approval = getApproval(approvalId);

        if (!APPROVAL_PENDING.equals(approval.getStatus())) {
            throw new BusinessException(
                    ErrorCode.CONFLICT,
                    "Yêu cầu duyệt không còn ở trạng thái PENDING"
            );
        }

        ProductRevision revision = productRevisionRepository
                .findById(approval.getRevisionId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "ProductRevision",
                        approval.getRevisionId()
                ));

        Product product = productRepository.findById(revision.getProductId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Product",
                                revision.getProductId()
                        ));

        ensureCurrentPendingRevision(product, revision);

        LocalDateTime now = LocalDateTime.now();
        ProductStatus previousStatus = product.getStatus();

        approval.setStatus(APPROVAL_REJECTED);
        approval.setApprovedBy(actorId);
        approval.setApprovedAt(now);
        approval.setReason(reason);

        revision.setStatus(REVISION_REJECTED);
        revision.setRejectionReason(reason);

        // Nếu từng có bản xuất bản thì bản cũ vẫn được bán.
        // Nếu chưa có bản nào thì sản phẩm quay về DRAFT.
        if (product.getPublishedRevisionId() != null) {
            product.setStatus(ProductStatus.ACTIVE);
        } else {
            product.setStatus(ProductStatus.DRAFT);
        }

        publicationApprovalRepository.save(approval);
        productRevisionRepository.save(revision);
        productRepository.save(product);

        saveHistory(
                product.getId(),
                revision.getId(),
                previousStatus,
                product.getStatus(),
                "PUBLICATION_REJECTED: " + truncate(reason, 475),
                actorId,
                now
        );

        saveOutboxEvent(
                product.getId(),
                revision.getId(),
                "ProductPublicationRejected",
                actorId,
                approval.getId(),
                now
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductPublishingHistoryResponse> getPublishingHistory(
            UUID productId
    ) {
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product", productId);
        }

        return publishingHistoryRepository
                .findByProductIdOrderByChangedAtDesc(productId)
                .stream()
                .map(publishingHistoryMapper::toResponse)
                .toList();
    }

    private PublicationApproval getApproval(UUID approvalId) {
        return publicationApprovalRepository.findById(approvalId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "PublicationApproval",
                                approvalId
                        ));
    }

    private void ensureCurrentPendingRevision(
            Product product,
            ProductRevision revision
    ) {
        if (!REVISION_PENDING_APPROVAL.equals(revision.getStatus())) {
            throw new BusinessException(
                    ErrorCode.CONFLICT,
                    "Revision không ở trạng thái PENDING_APPROVAL"
            );
        }

        if (!revision.getId().equals(product.getCurrentRevisionId())) {
            throw new BusinessException(
                    ErrorCode.CONFLICT,
                    "Revision này không còn là revision hiện hành cần duyệt"
            );
        }

        if (product.getStatus() == ProductStatus.DISCONTINUED) {
            throw new BusinessException(
                    ErrorCode.CONFLICT,
                    "Không thể xuất bản sản phẩm đã ngừng kinh doanh"
            );
        }
    }

    private void saveHistory(
            UUID productId,
            UUID revisionId,
            ProductStatus fromStatus,
            ProductStatus toStatus,
            String reason,
            UUID actorId,
            LocalDateTime changedAt
    ) {
        ProductPublishingHistory history = ProductPublishingHistory.builder()
                .productId(productId)
                .revisionId(revisionId)
                .fromStatus(fromStatus)
                .toStatus(toStatus)
                .reason(truncate(reason, 500))
                .changedBy(actorId)
                .changedAt(changedAt)
                .build();

        publishingHistoryRepository.save(history);
    }

    private void saveOutboxEvent(
            UUID productId,
            UUID revisionId,
            String eventType,
            UUID actorId,
            UUID approvalId,
            LocalDateTime occurredAt
    ) {
        UUID eventId = UUID.randomUUID();

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("eventId", eventId);
        payload.put("eventType", eventType);
        payload.put("eventVersion", 1);
        payload.put("occurredAt", occurredAt);
        payload.put("productId", productId);
        payload.put("revisionId", revisionId);
        payload.put("approvalId", approvalId);
        payload.put("actorId", actorId);

        final String jsonPayload;

        try {
            jsonPayload = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(
                    ErrorCode.INTERNAL_ERROR,
                    "Không thể tạo payload cho sự kiện Outbox"
            );
        }

        OutboxEvent event = OutboxEvent.builder()
                .eventId(eventId)
                .aggregateType("PRODUCT")
                .aggregateId(productId)
                .eventType(eventType)
                .eventVersion(1)
                .topic(OUTBOX_TOPIC)
                .payload(jsonPayload)
                .status(OutboxStatus.PENDING)
                .retryCount(0)
                .occurredAt(occurredAt)
                .build();

        outboxEventRepository.save(event);
    }

    private void requireActor(UUID actorId) {
        if (actorId == null) {
            throw new BusinessException(
                    ErrorCode.BAD_REQUEST,
                    "Không xác định được người thực hiện thao tác"
            );
        }
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
