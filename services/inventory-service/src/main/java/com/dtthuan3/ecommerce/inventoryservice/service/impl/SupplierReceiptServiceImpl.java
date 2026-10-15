package com.dtthuan3.ecommerce.inventoryservice.service.impl;

import com.dtthuan3.ecommerce.common.exception.BusinessException;
import com.dtthuan3.ecommerce.common.exception.ErrorCode;

import com.dtthuan3.ecommerce.inventoryservice.dto.request.CreateSupplierReceiptRequest;
import com.dtthuan3.ecommerce.inventoryservice.dto.request.UpdateSupplierReceiptActualsRequest;
import com.dtthuan3.ecommerce.inventoryservice.dto.response.SupplierReceiptResponse;
import com.dtthuan3.ecommerce.inventoryservice.dto.response.SupplierReceiptSummaryResponse;
import com.dtthuan3.ecommerce.inventoryservice.repository.SupplierReceiptRepository;
import com.dtthuan3.ecommerce.inventoryservice.repository.model.SupplierReceiptHeader;
import com.dtthuan3.ecommerce.inventoryservice.repository.model.SupplierReceiptLineRecord;
import com.dtthuan3.ecommerce.inventoryservice.service.SupplierReceiptService;
import com.dtthuan3.ecommerce.inventoryservice.port.ActiveSkuValidator;
import com.dtthuan3.ecommerce.inventoryservice.port.ActiveWarehouseValidator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.UUID;

/**
 * Điều phối nghiệp vụ mở phiếu nhập DRAFT.
 * Service phụ thuộc các interface repository/port để có thể thay JDBC adapter hoặc adapter tích hợp
 * mà không làm controller và quy tắc nghiệp vụ phụ thuộc vào công nghệ cụ thể.
 */
@Service
@RequiredArgsConstructor
public class SupplierReceiptServiceImpl implements SupplierReceiptService {
    /** Repository interface; adapter JDBC chịu trách nhiệm SQL và ánh xạ bản ghi. */
    private final SupplierReceiptRepository receiptRepository;

    /** Port xác thực kho, được hiện thực bởi adapter đọc Warehouse projection. */
    private final ActiveWarehouseValidator warehouseValidator;

    /** Port kiểm tra SKU, được hiện thực bởi adapter Product Service/projection. */
    private final ActiveSkuValidator skuValidator;

    /** Bộ chuyển đổi payload chuẩn hóa thành byte trước khi tính SHA-256 idempotency. */
    private final ObjectMapper objectMapper;

    /** Ranh giới transaction cục bộ bao trùm header, lines và document references. */
    private final TransactionTemplate transactionTemplate;

    /** Đọc phiếu cùng toàn bộ dữ liệu con để response API luôn có cấu trúc đầy đủ. */
    @Override
    @Transactional(readOnly = true)
    public SupplierReceiptResponse getById(String receiptId) {
        // Bước 1: tìm header; ID không tồn tại được trả thành lỗi 404 qua BusinessException chung.
        SupplierReceiptHeader header = receiptRepository.findById(receiptId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND,
                        "Không tìm thấy phiếu nhập: " + receiptId));

        // Bước 2: đọc các dòng theo line_number để giữ nguyên thứ tự đã nhập trên phiếu.
        List<SupplierReceiptLineRecord> lines = receiptRepository.findLines(receiptId);

        // Bước 3: đọc chứng từ đã gắn; thứ tự ổn định giúp các lần GET trả nội dung nhất quán.
        List<String> assetIds = receiptRepository.findDocumentAssetIds(receiptId);

        // Bước 4: ẩn payload hash và model lưu trữ, chỉ trả DTO công khai của API.
        return toResponse(header, lines, assetIds);
    }

    /** Đọc danh sách phiếu theo kho; dữ liệu dòng hàng chi tiết chỉ tải khi gọi API getById. */
    @Override
    @Transactional(readOnly = true)
    public Page<SupplierReceiptSummaryResponse> listByWarehouse(String warehouseId, Pageable pageable) {
        // Chuẩn hóa ID để tránh truy vấn sai chỉ vì khoảng trắng đầu/cuối từ query/path parameter.
        return receiptRepository.findByWarehouseId(warehouseId.trim(), pageable);
    }

    /**
     * Kiểm tra dữ liệu phụ thuộc bên ngoài trước, sau đó mới mở transaction ghi ngắn vào Inventory.
     * Nếu operationId đã được xử lý, kết quả cũ được trả lại mà không gọi lại Product Service;
     * nếu khóa đó gắn với payload khác thì trả 409 để tránh tạo hai kết quả mâu thuẫn.
     */
    @Override
    public SupplierReceiptResponse create(String warehouseId, CreateSupplierReceiptRequest request) {
        String normalizedWarehouseId = warehouseId.trim();
        List<String> skuCodes = request.lines().stream()
                .map(line -> line.skuId().trim())
                .distinct()
                .toList();
        List<String> assetIds = request.documentAssetIds() == null ? List.of()
                : request.documentAssetIds().stream().map(String::trim).sorted().toList();

        // Tài liệu là tập liên kết, không mang thứ tự nghiệp vụ; sắp xếp để retry trả response như nhau.
        if (assetIds.stream().distinct().count() != assetIds.size()) {
            throw new BusinessException(ErrorCode.INVALID_OPERATION_CONTENT,
                    "documentAssetIds không được chứa giá trị trùng");
        }

        String operationId = request.operationId().trim();
        String payloadHash = calculatePayloadHash(normalizedWarehouseId, request, assetIds);
        SupplierReceiptHeader previous = receiptRepository.findByOperationId(operationId).orElse(null);
        if (previous != null) {
            return replayOrReject(previous, payloadHash);
        }

        // Xác thực SKU có thể gọi Product Service nên chạy ngoài transaction ghi để không giữ khóa DB qua mạng.
        skuValidator.requireActiveSkus(skuCodes);

        // Header, lines và document references được ghi trong cùng transaction JDBC cục bộ.
        return transactionTemplate.execute(transactionStatus -> createInsideTransaction(
                normalizedWarehouseId, request, assetIds, operationId, payloadHash));
    }

    /**
     * Tạo dữ liệu trong một transaction ngắn. Unique index và INSERT ... ON CONFLICT đóng khoảng đua
     * giữa bước xác thực dependency và lúc ghi mà không cần khóa phân tán hoặc truy vấn khóa riêng.
     */
    private SupplierReceiptResponse createInsideTransaction(String warehouseId,
                                                             CreateSupplierReceiptRequest request,
                                                             List<String> assetIds,
                                                             String operationId,
                                                             String payloadHash) {
        // Khóa SHARE Warehouse trong chính transaction tạo phiếu để trạng thái không thể đổi giữa kiểm tra và commit.
        warehouseValidator.requireKnownActiveWarehouse(warehouseId);

        String receiptId = UUID.randomUUID().toString();
        SupplierReceiptHeader candidate = new SupplierReceiptHeader(
                receiptId,
                warehouseId,
                trimToNull(request.supplierRef()),
                operationId,
                payloadHash,
                "DRAFT",
                0L,
                // PostgreSQL TIMESTAMPTZ lưu độ chính xác microsecond; chuẩn hóa để response retry giống lần đầu.
                Instant.now().truncatedTo(ChronoUnit.MICROS));

        // INSERT ... ON CONFLICT là thao tác claim operationId nguyên tử trong PostgreSQL.
        boolean created = receiptRepository.insertHeaderIfAbsent(candidate);
        if (!created) {
            // Request khác đã tạo cùng operationId trong lúc kiểm tra dependency; chỉ đọc lại kết quả thắng.
            SupplierReceiptHeader existing = receiptRepository.findByOperationId(operationId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_ERROR,
                            "Không đọc lại được phiếu nhập sau khi tranh chấp operationId"));
            return replayOrReject(existing, payloadHash);
        }

        // Header vừa insert đã có đầy đủ dữ liệu; dùng lại bản ghi này để tránh thêm một SELECT.
        SupplierReceiptHeader header = candidate;

        // Đánh số theo đúng thứ tự request để người dùng đối chiếu phiếu trên màn hình hoặc chứng từ.
        List<SupplierReceiptLineRecord> lines = new java.util.ArrayList<>(request.lines().size());
        for (int index = 0; index < request.lines().size(); index++) {
            CreateSupplierReceiptRequest.Line input = request.lines().get(index);
            lines.add(new SupplierReceiptLineRecord(
                    UUID.randomUUID().toString(),
                    index + 1,
                    input.skuId().trim(),
                    input.uom().trim(),
                    input.expectedQty().setScale(4),
                    trimToNull(input.lotCode()),
                    BigDecimal.ZERO.setScale(4),
                    BigDecimal.ZERO.setScale(4),
                    BigDecimal.ZERO.setScale(4),
                    BigDecimal.ZERO.setScale(4)));
        }
        receiptRepository.insertLines(receiptId, lines);

        // Đây là liên kết ID; trạng thái READY của file sẽ được kiểm tra trước khi submit phiếu.
        receiptRepository.insertDocuments(receiptId, assetIds);
        return toResponse(header, lines, assetIds);
    }

    /** So sánh hash theo constant-time và trả lại chính dữ liệu phiếu khi request được gửi lặp. */
    private SupplierReceiptResponse replayOrReject(SupplierReceiptHeader header, String requestedPayloadHash) {
        boolean samePayload = MessageDigest.isEqual(
                header.payloadHash().getBytes(StandardCharsets.US_ASCII),
                requestedPayloadHash.getBytes(StandardCharsets.US_ASCII));
        if (!samePayload) {
            throw new BusinessException(ErrorCode.CONFLICT,
                    "operationId đã được dùng cho payload khác");
        }

        // Phiếu đã tồn tại nên đọc các phần con từ DB, không tái tạo ID hoặc chèn movement lần nữa.
        return toResponse(header,
                receiptRepository.findLines(header.id()),
                receiptRepository.findDocumentAssetIds(header.id()));
    }

    /** Chuyển các bản ghi JDBC thành response công khai; không để lộ payload hash hoặc cấu trúc DB. */
    private SupplierReceiptResponse toResponse(SupplierReceiptHeader header,
                                               List<SupplierReceiptLineRecord> lines,
                                               List<String> assetIds) {
        List<SupplierReceiptResponse.Line> responseLines = lines.stream()
                .map(line -> new SupplierReceiptResponse.Line(line.lineId(), line.lineNumber(), line.skuId(),
                        line.uom(), line.expectedQty(), line.lotCode(), line.actualQty(), line.goodQty(),
                        line.damagedQty(), line.inspectionQty()))
                .toList();
        return new SupplierReceiptResponse(header.id(), header.warehouseId(), header.supplierRef(),
                header.status(), header.version(), header.operationId(), responseLines, assetIds, header.createdAt());
    }

    /**
     * Tạo dấu vân tay SHA-256 từ payload đã chuẩn hóa, giữ thứ tự dòng hàng nhưng bỏ khác biệt
     * không mang ý nghĩa như khoảng trắng ngoài chuỗi hoặc 1.0 so với 1.000.
     */
    private String calculatePayloadHash(String warehouseId, CreateSupplierReceiptRequest request,
                                        List<String> assetIds) {
        try {
            List<IdempotencyLine> lines = request.lines().stream()
                    .map(line -> new IdempotencyLine(line.skuId().trim(), line.uom().trim(),
                            line.expectedQty().stripTrailingZeros(), trimToNull(line.lotCode())))
                    .toList();
            byte[] canonicalPayload = objectMapper.writeValueAsBytes(new IdempotencyPayload(
                    warehouseId, trimToNull(request.supplierRef()), lines, assetIds));
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(canonicalPayload);
            return HexFormat.of().formatHex(digest);
        } catch (JsonProcessingException | NoSuchAlgorithmException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR,
                    "Không thể tạo dấu vân tay idempotency cho phiếu nhập");
        }
    }

    /** Chuẩn hóa chuỗi tùy chọn; chuỗi trắng được lưu như giá trị SQL NULL. */
    private String trimToNull(String value) {
        if (value == null) return null;
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    /** Cấu trúc tuần tự hóa ổn định dành riêng cho phép băm idempotency. */
    private record IdempotencyPayload(String warehouseId, String supplierRef,
                                      List<IdempotencyLine> lines, List<String> documentAssetIds) {}

    /** Các trường của một dòng hàng có ảnh hưởng đến tính đồng nhất của thao tác tạo phiếu. */
    private record IdempotencyLine(String skuCode, String uom, BigDecimal expectedQty, String lotCode) {}

    /**
     * Cập nhật kết quả kiểm đếm cho phiếu còn DRAFT. Khóa header, version, dòng hàng và tài liệu
     * được xử lý trong một transaction để không thể chỉ cập nhật một phần phiếu.
     */
    @Override
    public SupplierReceiptResponse updateActuals(String receiptId, UpdateSupplierReceiptActualsRequest request) {
        return transactionTemplate.execute(transactionStatus -> updateActualsInsideTransaction(receiptId, request));
    }

    /** Phần xử lý bên trong transaction; SELECT FOR UPDATE là điểm tuần tự hóa cập nhật cùng receipt. */
    private SupplierReceiptResponse updateActualsInsideTransaction(
            String receiptId, UpdateSupplierReceiptActualsRequest request) {
        SupplierReceiptHeader header = receiptRepository.findByIdForUpdate(receiptId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND,
                        "Không tìm thấy phiếu nhập: " + receiptId));

        if (!"DRAFT".equals(header.status())) {
            throw new BusinessException(ErrorCode.INVALID_RESOURCE_STATE,
                    "Chỉ được cập nhật số lượng thực nhận khi phiếu còn ở trạng thái DRAFT");
        }
        // version trong model là long nguyên thủy, còn expectedVersion là Long từ request DTO.
        // So sánh giá trị sau khi chặn null để tránh gọi equals() trên kiểu nguyên thủy.
        if (request.expectedVersion() == null || header.version() != request.expectedVersion().longValue()) {
            throw new BusinessException(ErrorCode.VERSION_CONFLICT,
                    "Phiếu đã được cập nhật bởi người khác; hãy tải lại dữ liệu mới nhất");
        }

        List<SupplierReceiptLineRecord> currentLines = receiptRepository.findLines(receiptId);
        Map<String, SupplierReceiptLineRecord> currentById = currentLines.stream()
                .collect(Collectors.toMap(SupplierReceiptLineRecord::lineId, Function.identity()));
        Set<String> requestedIds = new HashSet<>();
        for (UpdateSupplierReceiptActualsRequest.Line input : request.lines()) {
            if (!requestedIds.add(input.lineId())) {
                throw new BusinessException(ErrorCode.INVALID_OPERATION_CONTENT,
                        "Danh sách lines không được chứa lineId trùng: " + input.lineId());
            }
            if (!currentById.containsKey(input.lineId())) {
                throw new BusinessException(ErrorCode.INVALID_OPERATION_CONTENT,
                        "lineId không thuộc phiếu nhập này: " + input.lineId());
            }
            BigDecimal classified = input.goodQty().add(input.damagedQty()).add(input.inspectionQty());
            if (classified.compareTo(input.actualQty()) != 0) {
                throw new BusinessException(ErrorCode.INVALID_QUANTITY,
                        "goodQty + damagedQty + inspectionQty phải bằng actualQty tại dòng " + input.lineId());
            }
        }
        if (!requestedIds.equals(currentById.keySet())) {
            throw new BusinessException(ErrorCode.INVALID_OPERATION_CONTENT,
                    "Request phải gửi đúng một lần cho toàn bộ dòng hiện có của phiếu");
        }

        List<String> evidenceAssetIds = request.evidenceAssetIds() == null ? List.of()
                : request.evidenceAssetIds().stream().map(String::trim).sorted().toList();
        if (evidenceAssetIds.stream().distinct().count() != evidenceAssetIds.size()) {
            throw new BusinessException(ErrorCode.INVALID_OPERATION_CONTENT,
                    "evidenceAssetIds không được chứa giá trị trùng");
        }

        if (!receiptRepository.incrementVersion(receiptId, request.expectedVersion())) {
            throw new BusinessException(ErrorCode.VERSION_CONFLICT,
                    "Phiếu đã thay đổi trạng thái hoặc version trong lúc cập nhật");
        }

        Map<String, UpdateSupplierReceiptActualsRequest.Line> requestedById = request.lines().stream()
                .collect(Collectors.toMap(UpdateSupplierReceiptActualsRequest.Line::lineId, Function.identity()));
        List<SupplierReceiptLineRecord> updatedLines = currentLines.stream().map(current -> {
            UpdateSupplierReceiptActualsRequest.Line input = requestedById.get(current.lineId());
            return new SupplierReceiptLineRecord(current.lineId(), current.lineNumber(), current.skuId(),
                    current.uom(), current.expectedQty(), current.lotCode(), input.actualQty().setScale(4),
                    input.goodQty().setScale(4), input.damagedQty().setScale(4), input.inspectionQty().setScale(4));
        }).toList();

        receiptRepository.updateActuals(receiptId, updatedLines);
        receiptRepository.appendDocuments(receiptId, evidenceAssetIds);

        // Đọc lại các liên kết để response chứa cả chứng từ lúc tạo và bằng chứng vừa bổ sung.
        SupplierReceiptHeader updatedHeader = new SupplierReceiptHeader(header.id(), header.warehouseId(),
                header.supplierRef(), header.operationId(), header.payloadHash(), header.status(),
                request.expectedVersion() + 1, header.createdAt());
        return toResponse(updatedHeader, updatedLines, receiptRepository.findDocumentAssetIds(receiptId));
    }
}
