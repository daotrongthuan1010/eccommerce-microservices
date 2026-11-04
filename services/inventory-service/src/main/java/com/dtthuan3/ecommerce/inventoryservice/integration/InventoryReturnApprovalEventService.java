package com.dtthuan3.ecommerce.inventoryservice.integration;

import com.dtthuan3.ecommerce.common.exception.BusinessException;
import com.dtthuan3.ecommerce.common.exception.ErrorCode;
import com.dtthuan3.ecommerce.inventoryservice.integration.event.ReturnLineApprovalEvent;
import com.dtthuan3.ecommerce.inventoryservice.repository.InventoryEventInboxRepository;
import com.dtthuan3.ecommerce.inventoryservice.repository.InventoryReturnReceiptRepository;
import com.dtthuan3.ecommerce.inventoryservice.repository.InventoryReturnReceiptRepository.ApprovedReturnLine;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Set;

/**
 * Nhận quyết định trả hàng từ Return/Order và lưu projection cùng dấu inbox trong một transaction.
 * Sự kiện chỉ tạo hạn mức sau khi đã kiểm tra hợp đồng, nguồn event, version và ràng buộc enterprise/SKU/order line;
 * request nhận parcel không có quyền tự tạo hay nâng hạn mức này.
 */
@Service
@RequiredArgsConstructor
public class InventoryReturnApprovalEventService {
    private static final String CONSUMER_NAME = "inventory-return-approval-projection-v1";
    private static final Set<String> SUPPORTED_TYPES = Set.of(
            "ReturnLineApproved", "ReturnLineApprovalUpdated");

    private final InventoryEventInboxRepository inboxRepository;
    private final InventoryReturnReceiptRepository returnReceiptRepository;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    /** Claim event và cập nhật hạn mức nguyên tử; lỗi projection làm rollback inbox để Kafka retry an toàn. */
    @Transactional
    public Outcome apply(ReturnLineApprovalEvent event) {
        validate(event);
        String payloadHash = payloadHash(event);
        InventoryEventInboxRepository.InboxClaim claim = inboxRepository.claim(
                CONSUMER_NAME, event.eventId(), payloadHash, Instant.now());
        if (!claim.inserted()) {
            if (!MessageDigest.isEqual(claim.existingPayloadHash().getBytes(StandardCharsets.US_ASCII),
                    payloadHash.getBytes(StandardCharsets.US_ASCII))) {
                meterRegistry.counter("inventory.return_approval.event_id_conflict").increment();
                throw new BusinessException(ErrorCode.CONFLICT,
                        "Event trả hàng đã dùng eventId này với payload khác");
            }
            meterRegistry.counter("inventory.return_approval.duplicates").increment();
            return Outcome.DUPLICATE;
        }

        InventoryReturnReceiptRepository.ApprovalApplyResult result = returnReceiptRepository.applyApproval(
                new ApprovedReturnLine(event.returnRequestId().trim(), event.orderLineId().trim(),
                        event.enterpriseId().trim(), event.sku().trim(), event.approvedQuantity(),
                        event.sourceVersion(), event.eventId().trim()));
        if (result == InventoryReturnReceiptRepository.ApprovalApplyResult.CONFLICT) {
            meterRegistry.counter("inventory.return_approval.conflicts").increment();
            throw new BusinessException(ErrorCode.CONFLICT,
                    "Quyết định phê duyệt xung đột với hạn mức hoặc hàng đã nhận hiện tại");
        }
        if (result == InventoryReturnReceiptRepository.ApprovalApplyResult.STALE) {
            meterRegistry.counter("inventory.return_approval.stale").increment();
            return Outcome.STALE;
        }
        meterRegistry.counter("inventory.return_approval.applied").increment();
        return result == InventoryReturnReceiptRepository.ApprovalApplyResult.DUPLICATE
                ? Outcome.DUPLICATE : Outcome.APPLIED;
    }

    /** Bắt payload thiếu/sai trước khi đưa quyết định vào projection của Inventory. */
    private void validate(ReturnLineApprovalEvent event) {
        if (event == null || blank(event.eventId()) || event.eventId().length() > 200
                || event.eventType() == null || !SUPPORTED_TYPES.contains(event.eventType())
                || blank(event.returnRequestId()) || event.returnRequestId().length() > 100
                || blank(event.orderLineId()) || event.orderLineId().length() > 100
                || blank(event.enterpriseId()) || event.enterpriseId().length() > 100
                || blank(event.sku()) || event.sku().length() > 100
                || event.approvedQuantity() == null || event.approvedQuantity().signum() <= 0
                || event.approvedQuantity().stripTrailingZeros().scale() > 4
                || event.approvedQuantity().precision() - event.approvedQuantity().scale() > 15
                || event.sourceVersion() == null
                || event.sourceVersion() < 0 || event.occurredAt() == null) {
            throw new IllegalArgumentException("Return line approval event thiếu trường hoặc sai định dạng");
        }
    }

    private String payloadHash(ReturnLineApprovalEvent event) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(objectMapper.writeValueAsBytes(event)));
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Không thể chuẩn hóa event phê duyệt trả hàng", exception);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("JVM không hỗ trợ SHA-256", exception);
        }
    }

    private boolean blank(String value) { return value == null || value.isBlank(); }

    /** Kết quả để consumer phân biệt event mới, trùng và event đến sai thứ tự. */
    public enum Outcome { APPLIED, DUPLICATE, STALE }
}
