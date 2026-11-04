package com.dtthuan3.ecommerce.shippingservice.service;

import com.dtthuan3.ecommerce.common.exception.BusinessException;
import com.dtthuan3.ecommerce.common.exception.ErrorCode;
import com.dtthuan3.ecommerce.common.exception.ResourceNotFoundException;
import com.dtthuan3.ecommerce.shippingservice.config.ShippingProperties;
import com.dtthuan3.ecommerce.shippingservice.domain.ShipmentStatus;
import com.dtthuan3.ecommerce.shippingservice.domain.ShippingFeeCalculator;
import com.dtthuan3.ecommerce.shippingservice.dto.request.CarrierWebhookRequest;
import com.dtthuan3.ecommerce.shippingservice.dto.request.CreateQuoteRequest;
import com.dtthuan3.ecommerce.shippingservice.dto.request.CreateShipmentRequest;
import com.dtthuan3.ecommerce.shippingservice.dto.response.QuoteResponse;
import com.dtthuan3.ecommerce.shippingservice.dto.response.ShipmentResponse;
import com.dtthuan3.ecommerce.shippingservice.entity.Shipment;
import com.dtthuan3.ecommerce.shippingservice.entity.ShipmentStatusHistory;
import com.dtthuan3.ecommerce.shippingservice.entity.ShippingOutbox;
import com.dtthuan3.ecommerce.shippingservice.entity.ShippingQuote;
import com.dtthuan3.ecommerce.shippingservice.provider.CarrierProvider;
import com.dtthuan3.ecommerce.shippingservice.repository.ShipmentRepository;
import com.dtthuan3.ecommerce.shippingservice.repository.ShipmentStatusHistoryRepository;
import com.dtthuan3.ecommerce.shippingservice.repository.ShippingOutboxRepository;
import com.dtthuan3.ecommerce.shippingservice.repository.ShippingQuoteRepository;
import com.dtthuan3.ecommerce.shippingservice.util.HmacSigner;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Core shipping. Nguyen tac:
 * - Quote la nguon su that ve phi: co han 15 phut, dung 1 lan, phi phai khop expectedFee cua order.
 * - Webhook carrier: verify HMAC truoc, trung trang thai thi no-op, trang thai "lui" thi bo qua + log.
 */
@Service
public class ShippingService {

    private static final Logger log = LoggerFactory.getLogger(ShippingService.class);
    public static final String TOPIC = "shipping-events";
    public static final int MAX_DELIVERY_ATTEMPTS = 3;

    private final ShippingQuoteRepository quotes;
    private final ShipmentRepository shipments;
    private final ShipmentStatusHistoryRepository history;
    private final ShippingOutboxRepository outbox;
    private final ShippingFeeCalculator feeCalculator;
    private final CarrierProvider carrier;
    private final ShippingProperties props;
    private final ObjectMapper objectMapper;

    public ShippingService(ShippingQuoteRepository quotes, ShipmentRepository shipments,
                           ShipmentStatusHistoryRepository history, ShippingOutboxRepository outbox,
                           ShippingFeeCalculator feeCalculator, CarrierProvider carrier,
                           ShippingProperties props, ObjectMapper objectMapper) {
        this.quotes = quotes;
        this.shipments = shipments;
        this.history = history;
        this.outbox = outbox;
        this.feeCalculator = feeCalculator;
        this.carrier = carrier;
        this.props = props;
        this.objectMapper = objectMapper;
    }

    // ---------------------------------------------------------------- quote

    @Transactional
    public QuoteResponse createQuote(CreateQuoteRequest req) {
        var fee = feeCalculator.calculate(req.fromProvinceCode(), req.toProvinceCode(), req.weightGrams(),
                req.lengthCm(), req.widthCm(), req.heightCm(), req.levelOrDefault());
        ShippingQuote q = new ShippingQuote();
        q.setId(UUID.randomUUID().toString());
        q.setFromProvinceCode(req.fromProvinceCode());
        q.setToProvinceCode(req.toProvinceCode());
        q.setWeightGrams(req.weightGrams());
        q.setBillableWeightGrams(fee.billableWeightGrams());
        q.setLengthCm(req.lengthCm());
        q.setWidthCm(req.widthCm());
        q.setHeightCm(req.heightCm());
        q.setServiceLevel(req.levelOrDefault());
        q.setZone(fee.zone().name());
        q.setFee(fee.totalFee());
        q.setExpiresAt(Instant.now().plus(props.getQuoteTtl()));
        quotes.save(q);
        return QuoteResponse.from(q);
    }

    // ---------------------------------------------------------------- shipment

    public ShipmentResponse create(CreateShipmentRequest req, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Idempotency-Key header is required");
        }
        String hash = HmacSigner.sha256Hex(req.orderId() + "|" + req.quoteId() + "|" + req.expectedFee()
                + "|" + req.codAmount() + "|" + req.recipient().phone());
        var existing = shipments.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            if (!existing.get().getRequestHash().equals(hash)) {
                throw new BusinessException("IDEMPOTENCY_KEY_REUSED", HttpStatus.CONFLICT,
                        "Idempotency-Key already used with a different payload");
            }
            return ShipmentResponse.from(existing.get());
        }

        ShippingQuote quote = quotes.findById(req.quoteId())
                .orElseThrow(() -> new ResourceNotFoundException("Quote not found: " + req.quoteId()));
        if (Instant.now().isAfter(quote.getExpiresAt())) {
            throw new BusinessException("SHIPPING_QUOTE_EXPIRED", HttpStatus.CONFLICT,
                    "Quote " + quote.getId() + " expired at " + quote.getExpiresAt() + "; request a new quote");
        }
        if (quote.getConsumedByShipmentId() != null || shipments.existsByQuoteId(quote.getId())) {
            throw new BusinessException("SHIPPING_QUOTE_ALREADY_USED", HttpStatus.CONFLICT,
                    "Quote " + quote.getId() + " was already used to create a shipment");
        }
        if (req.expectedFee() != quote.getFee()) {
            throw new BusinessException("SHIPPING_FEE_CHANGED", HttpStatus.CONFLICT,
                    "Shipping fee changed: expected " + req.expectedFee() + " but quote fee is " + quote.getFee());
        }

        Shipment s = new Shipment();
        s.setId(UUID.randomUUID().toString());
        s.setOrderId(req.orderId());
        s.setIdempotencyKey(idempotencyKey);
        s.setRequestHash(hash);
        s.setQuoteId(quote.getId());
        s.setCarrier(carrier.carrierCode());
        s.setStatus(ShipmentStatus.CREATED);
        s.setServiceLevel(quote.getServiceLevel());
        s.setShippingFee(quote.getFee());
        s.setCodAmount(req.codAmount());
        s.setWeightGrams(quote.getWeightGrams());
        s.setSenderName(req.sender().name());
        s.setSenderPhone(req.sender().phone());
        s.setSenderAddress(req.sender().address());
        s.setSenderProvinceCode(req.sender().provinceCode());
        s.setRecipientName(req.recipient().name());
        s.setRecipientPhone(req.recipient().phone());
        s.setRecipientAddress(req.recipient().address());
        s.setRecipientProvinceCode(req.recipient().provinceCode());
        s.setNote(req.note());
        shipments.save(s);
        recordHistory(s, "Shipment created");

        try {
            s.setCarrierCode(carrier.createShipment(s));
            shipments.save(s);
        } catch (BusinessException e) {
            s.setStatus(ShipmentStatus.CREATION_FAILED);
            s.setFailureReason(e.getMessage());
            shipments.save(s);
            recordHistory(s, "Carrier failed to create shipment: " + e.getMessage());
            throw e;
        }

        quote.setConsumedByShipmentId(s.getId());
        quotes.save(quote);
        emit(s, "ShipmentCreated");
        return ShipmentResponse.from(s);
    }

    @Transactional(readOnly = true)
    public ShipmentResponse get(String id) {
        return ShipmentResponse.from(find(id));
    }

    @Transactional(readOnly = true)
    public ShipmentResponse getByOrder(String orderId) {
        return ShipmentResponse.from(shipments.findFirstByOrderIdOrderByCreatedAtDesc(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found for order " + orderId)));
    }

    @Transactional(readOnly = true)
    public List<ShipmentStatusHistory> getHistory(String id) {
        find(id);
        return history.findByShipmentIdOrderByOccurredAtAsc(id);
    }

    @Transactional
    public ShipmentResponse cancel(String id) {
        Shipment s = find(id);
        transition(s, ShipmentStatus.CANCELLED);
        shipments.save(s);
        recordHistory(s, "Cancelled by seller/order before pickup");
        if (s.getCarrierCode() != null) carrier.cancelShipment(s.getCarrierCode());
        emit(s, "ShipmentCancelled");
        return ShipmentResponse.from(s);
    }

    // ---------------------------------------------------------------- carrier webhook

    @Transactional
    public ShipmentResponse handleCarrierWebhook(CarrierWebhookRequest wh) {
        if (!carrier.verifyWebhookSignature(wh.carrierCode(), wh.status(), wh.timestamp(), wh.signature())
                || Math.abs(Instant.now().getEpochSecond() - wh.timestamp()) > 300) {
            throw new BusinessException("SIGNATURE_INVALID", HttpStatus.BAD_REQUEST,
                    "Carrier webhook signature is invalid; shipment state unchanged");
        }
        Shipment s = shipments.findByCarrierCode(wh.carrierCode())
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found for carrierCode " + wh.carrierCode()));

        ShipmentStatus target;
        try {
            target = ShipmentStatus.valueOf(wh.status().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Unknown shipment status: " + wh.status());
        }

        if (target == s.getStatus()) {
            log.info("Duplicate carrier webhook ignored shipment={} status={}", s.getId(), target);
            return ShipmentResponse.from(s);
        }
        boolean returnFlow = target == ShipmentStatus.RETURNING || target == ShipmentStatus.RETURNED;
        if (!s.getStatus().canTransitionTo(target)) {
            // Webhook den muon/sai thu tu: bo qua de khong lui trang thai, van tra 200 de carrier dung retry.
            log.warn("Out-of-order carrier webhook ignored shipment={} {} -> {}", s.getId(), s.getStatus(), target);
            return ShipmentResponse.from(s);
        }
        if (!returnFlow && target.progressRank() < s.getStatus().progressRank()) {
            log.warn("Stale carrier webhook ignored shipment={} {} -> {}", s.getId(), s.getStatus(), target);
            return ShipmentResponse.from(s);
        }

        s.setStatus(target);
        if (target == ShipmentStatus.DELIVERY_FAILED) {
            s.setDeliveryAttempts(s.getDeliveryAttempts() + 1);
            s.setFailureReason(wh.note());
        }
        shipments.save(s);
        recordHistory(s, wh.note());

        String eventType = switch (target) {
            case DELIVERED -> "ShipmentDelivered";
            case DELIVERY_FAILED -> "ShipmentDeliveryFailed";
            case RETURNED -> "ShipmentReturned";
            case CANCELLED -> "ShipmentCancelled";
            default -> "ShipmentStatusChanged";
        };
        emit(s, eventType);
        return ShipmentResponse.from(s);
    }

    // ---------------------------------------------------------------- helpers

    private void transition(Shipment s, ShipmentStatus target) {
        if (s.getStatus() == target) return;
        if (!s.getStatus().canTransitionTo(target)) {
            throw new BusinessException(ErrorCode.INVALID_RESOURCE_STATE,
                    "Shipment " + s.getId() + " cannot transition " + s.getStatus() + " -> " + target);
        }
        s.setStatus(target);
    }

    private Shipment find(String id) {
        return shipments.findById(id).orElseThrow(() -> new ResourceNotFoundException("Shipment not found: " + id));
    }

    private void recordHistory(Shipment s, String note) {
        ShipmentStatusHistory h = new ShipmentStatusHistory();
        h.setId(UUID.randomUUID().toString());
        h.setShipmentId(s.getId());
        h.setStatus(s.getStatus());
        h.setNote(note);
        history.save(h);
    }

    private void emit(Shipment s, String eventType) {
        String eventId = UUID.randomUUID().toString();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("eventId", eventId);
        payload.put("eventType", eventType);
        payload.put("shipmentId", s.getId());
        payload.put("orderId", s.getOrderId());
        payload.put("carrier", s.getCarrier());
        payload.put("carrierCode", s.getCarrierCode());
        payload.put("status", s.getStatus().name());
        payload.put("shippingFee", s.getShippingFee());
        payload.put("codAmount", s.getCodAmount());
        payload.put("deliveryAttempts", s.getDeliveryAttempts());
        payload.put("occurredAt", Instant.now().toString());

        ShippingOutbox o = new ShippingOutbox();
        o.setEventId(eventId);
        o.setAggregateId(s.getId());
        o.setEventType(eventType);
        o.setTopic(TOPIC);
        try {
            o.setPayload(objectMapper.writeValueAsString(payload));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialize payload", e);
        }
        outbox.save(o);
    }
}
