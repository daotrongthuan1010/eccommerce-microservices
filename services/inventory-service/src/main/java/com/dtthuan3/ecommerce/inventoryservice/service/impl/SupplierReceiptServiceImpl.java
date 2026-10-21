package com.dtthuan3.ecommerce.inventoryservice.service.impl;

import com.dtthuan3.ecommerce.common.exception.BusinessException;
import com.dtthuan3.ecommerce.common.exception.ErrorCode;

import com.dtthuan3.ecommerce.inventoryservice.dto.request.CreateSupplierReceiptRequest;
import com.dtthuan3.ecommerce.inventoryservice.dto.request.ApproveSupplierReceiptRequest;
import com.dtthuan3.ecommerce.inventoryservice.dto.request.UpdateSupplierReceiptActualsRequest;
import com.dtthuan3.ecommerce.inventoryservice.dto.request.PostSupplierReceiptRequest;
import com.dtthuan3.ecommerce.inventoryservice.dto.request.SubmitSupplierReceiptRequest;
import com.dtthuan3.ecommerce.inventoryservice.dto.response.SupplierReceiptResponse;
import com.dtthuan3.ecommerce.inventoryservice.dto.response.SupplierReceiptSummaryResponse;
import com.dtthuan3.ecommerce.inventoryservice.repository.SupplierReceiptRepository;
import com.dtthuan3.ecommerce.inventoryservice.repository.model.SupplierReceiptHeader;
import com.dtthuan3.ecommerce.inventoryservice.repository.model.SupplierReceiptLineRecord;
import com.dtthuan3.ecommerce.inventoryservice.service.SupplierReceiptService;
import com.dtthuan3.ecommerce.inventoryservice.service.InventoryService;
import com.dtthuan3.ecommerce.inventoryservice.service.ClassifiedStockReceipt;
import com.dtthuan3.ecommerce.inventoryservice.service.WarehouseManagementService;
import com.dtthuan3.ecommerce.inventoryservice.port.ActiveSkuValidator;
import com.dtthuan3.ecommerce.inventoryservice.port.ActiveWarehouseValidator;
import com.dtthuan3.ecommerce.inventoryservice.port.ReceiptDocumentValidator;
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
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.HashMap;
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

    /** Cộng tồn và ghi lịch sử nhập trong transaction đang ghi sổ receipt. */
    private final InventoryService inventoryService;

    /** Warehouse nằm trong cùng Inventory Service nên lấy enterpriseId trực tiếp, không gọi mạng. */
    private final WarehouseManagementService warehouseService;

    /** Port xác minh các file đính kèm trước khi phiếu chuyển sang bước chờ duyệt. */
    private final ReceiptDocumentValidator receiptDocumentValidator;

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
                Instant.now().truncatedTo(ChronoUnit.MICROS),
                null,
                null);

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
        List<SupplierReceiptLineRecord> lines = new ArrayList<>(request.lines().size());
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
                    BigDecimal.ZERO.setScale(4),
                    null));
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
                        line.damagedQty(), line.inspectionQty(), line.locationId()))
                .toList();
        return new SupplierReceiptResponse(header.id(), header.warehouseId(), header.supplierRef(),
                header.status(), header.version(), header.operationId(), responseLines, assetIds,
                header.createdAt(), header.approvedBy(), header.approvedAt());
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
                    input.goodQty().setScale(4), input.damagedQty().setScale(4), input.inspectionQty().setScale(4),
                    current.locationId());
        }).toList();

        receiptRepository.updateActuals(receiptId, updatedLines);
        receiptRepository.appendDocuments(receiptId, evidenceAssetIds);

        // Đọc lại các liên kết để response chứa cả chứng từ lúc tạo và bằng chứng vừa bổ sung.
        SupplierReceiptHeader updatedHeader = new SupplierReceiptHeader(header.id(), header.warehouseId(),
                header.supplierRef(), header.operationId(), header.payloadHash(), header.status(),
                request.expectedVersion() + 1, header.createdAt(), header.approvedBy(), header.approvedAt());
        return toResponse(updatedHeader, updatedLines, receiptRepository.findDocumentAssetIds(receiptId));
    }

    /**
     * Gửi phiếu sau kiểm đếm sang SUBMITTED. Kiểm tra File Service trước transaction,
     * sau đó khóa header và đối chiếu version lần nữa trước khi đổi trạng thái.
     */
    @Override
    public SupplierReceiptResponse submit(String receiptId, SubmitSupplierReceiptRequest request,
                                          String bearerToken) {
        SupplierReceiptHeader snapshot = receiptRepository.findById(receiptId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND,
                        "Không tìm thấy phiếu nhập: " + receiptId));

        // Retry sau timeout trả lại kết quả đã submit nếu version và trạng thái khớp lần gửi trước.
        if ("SUBMITTED".equals(snapshot.status())) {
            if (request.expectedVersion() != null && request.expectedVersion() + 1 == snapshot.version()) {
                return toResponse(snapshot, receiptRepository.findLines(receiptId),
                        receiptRepository.findDocumentAssetIds(receiptId));
            }
            throw new BusinessException(ErrorCode.INVALID_RESOURCE_STATE,
                    "Phiếu nhập đã được gửi sang trạng thái chờ duyệt");
        }
        if (!"DRAFT".equals(snapshot.status())) {
            throw new BusinessException(ErrorCode.INVALID_RESOURCE_STATE,
                    "Chỉ được gửi phiếu nhập ở trạng thái DRAFT");
        }
        if (request.expectedVersion() == null || snapshot.version() != request.expectedVersion().longValue()) {
            throw new BusinessException(ErrorCode.VERSION_CONFLICT,
                    "Phiếu đã thay đổi; hãy tải lại dữ liệu mới nhất trước khi gửi duyệt");
        }
        // Khi tạo mới version bằng 0; cập nhật actuals là bước bắt buộc trước khi gửi duyệt.
        if (snapshot.version() == 0) {
            throw new BusinessException(ErrorCode.INVALID_RESOURCE_STATE,
                    "Cần cập nhật số lượng thực nhận trước khi gửi phiếu sang chờ duyệt");
        }

        List<SupplierReceiptLineRecord> checkedLines = receiptRepository.findLines(receiptId);
        validateReceiptClassification(checkedLines);
        List<String> documentAssetIds = receiptRepository.findDocumentAssetIds(receiptId);
        receiptDocumentValidator.requireUploaded(documentAssetIds, bearerToken);

        // Gọi mạng xong mới mở transaction; tránh giữ khóa DB trong thời gian chờ File Service.
        return transactionTemplate.execute(status -> submitInsideTransaction(
                receiptId, request, checkedLines, documentAssetIds));
    }

    /** Khóa phiếu và kiểm tra lại dữ liệu sau khi đã xác minh file để chống race với request cập nhật khác. */
    private SupplierReceiptResponse submitInsideTransaction(String receiptId,
                                                             SubmitSupplierReceiptRequest request,
                                                             List<SupplierReceiptLineRecord> checkedLines,
                                                             List<String> checkedDocumentIds) {
        SupplierReceiptHeader header = receiptRepository.findByIdForUpdate(receiptId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND,
                        "Không tìm thấy phiếu nhập: " + receiptId));

        if (request.expectedVersion() == null
                || !"DRAFT".equals(header.status())
                || header.version() != request.expectedVersion().longValue()) {
            // Nếu một request đồng thời đã submit đúng version, retry này nhận lại kết quả đã commit.
            if ("SUBMITTED".equals(header.status())
                    && request.expectedVersion() != null
                    && request.expectedVersion() + 1 == header.version()) {
                return toResponse(header, receiptRepository.findLines(receiptId),
                        receiptRepository.findDocumentAssetIds(receiptId));
            }
            throw new BusinessException(ErrorCode.VERSION_CONFLICT,
                    "Phiếu đã thay đổi trong lúc xác minh chứng từ; hãy tải lại trước khi gửi duyệt");
        }

        List<String> currentDocumentIds = receiptRepository.findDocumentAssetIds(receiptId);
        if (!currentDocumentIds.equals(checkedDocumentIds)) {
            throw new BusinessException(ErrorCode.VERSION_CONFLICT,
                    "Danh sách chứng từ đã thay đổi trong lúc xác minh; hãy tải lại phiếu");
        }
        List<SupplierReceiptLineRecord> currentLines = receiptRepository.findLines(receiptId);
        if (!currentLines.equals(checkedLines)) {
            throw new BusinessException(ErrorCode.VERSION_CONFLICT,
                    "Số liệu phiếu đã thay đổi trong lúc xác minh; hãy tải lại phiếu");
        }
        validateReceiptClassification(currentLines);

        if (!receiptRepository.markSubmitted(receiptId, request.expectedVersion())) {
            throw new BusinessException(ErrorCode.VERSION_CONFLICT,
                    "Phiếu đã đổi trạng thái hoặc version trong lúc gửi duyệt");
        }
        SupplierReceiptHeader submittedHeader = new SupplierReceiptHeader(header.id(), header.warehouseId(),
                header.supplierRef(), header.operationId(), header.payloadHash(), "SUBMITTED",
                header.version() + 1, header.createdAt(), header.approvedBy(), header.approvedAt());
        return toResponse(submittedHeader, currentLines, currentDocumentIds);
    }

    /**
     * Duyệt phiếu đã submit trong transaction ngắn; ghi trạng thái, người duyệt và thời điểm cùng nhau.
     * Request lặp sau timeout chỉ được trả lại thành công khi cùng version và cùng người duyệt.
     */
    @Override
    public SupplierReceiptResponse approve(String receiptId, ApproveSupplierReceiptRequest request,
                                           String approvedBy) {
        return transactionTemplate.execute(status -> {
            SupplierReceiptHeader header = receiptRepository.findByIdForUpdate(receiptId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND,
                            "Không tìm thấy phiếu nhập: " + receiptId));

            if ("APPROVED".equals(header.status())
                    && request.expectedVersion() != null
                    && request.expectedVersion() + 1 == header.version()
                    && approvedBy != null
                    && approvedBy.equals(header.approvedBy())) {
                return toResponse(header, receiptRepository.findLines(receiptId),
                        receiptRepository.findDocumentAssetIds(receiptId));
            }
            if (!"SUBMITTED".equals(header.status())) {
                throw new BusinessException(ErrorCode.INVALID_RESOURCE_STATE,
                        "Chỉ được duyệt phiếu đang ở trạng thái SUBMITTED");
            }
            if (request.expectedVersion() == null || header.version() != request.expectedVersion().longValue()) {
                throw new BusinessException(ErrorCode.VERSION_CONFLICT,
                        "Phiếu đã thay đổi; hãy tải lại dữ liệu mới nhất trước khi duyệt");
            }
            if (approvedBy == null || approvedBy.isBlank()) {
                throw new BusinessException(ErrorCode.UNAUTHORIZED,
                        "Không xác định được tài khoản đang duyệt phiếu");
            }

            List<SupplierReceiptLineRecord> lines = receiptRepository.findLines(receiptId);
            validateReceiptClassification(lines);
            Instant approvedAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
            if (!receiptRepository.markApproved(receiptId, request.expectedVersion(), approvedBy, approvedAt)) {
                throw new BusinessException(ErrorCode.VERSION_CONFLICT,
                        "Phiếu đã đổi trạng thái hoặc version trong lúc duyệt");
            }

            SupplierReceiptHeader approvedHeader = new SupplierReceiptHeader(header.id(), header.warehouseId(),
                    header.supplierRef(), header.operationId(), header.payloadHash(), "APPROVED",
                    header.version() + 1, header.createdAt(), approvedBy, approvedAt);
            return toResponse(approvedHeader, lines, receiptRepository.findDocumentAssetIds(receiptId));
        });
    }

    /** Bảo đảm mỗi dòng đã kiểm đếm và toàn bộ số thực nhận đã được phân vào đúng các bucket chất lượng. */
    private void validateReceiptClassification(List<SupplierReceiptLineRecord> lines) {
        if (lines.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_OPERATION_CONTENT,
                    "Không thể gửi phiếu nhập không có dòng hàng");
        }
        for (SupplierReceiptLineRecord line : lines) {
            BigDecimal classified = line.goodQty().add(line.damagedQty()).add(line.inspectionQty());
            if (classified.compareTo(line.actualQty()) != 0) {
                throw new BusinessException(ErrorCode.INVALID_QUANTITY,
                        "Phân loại số lượng chưa khớp thực nhận tại dòng " + line.lineId());
            }
        }
    }

    /**
     * Ghi sổ phiếu APPROVED theo một transaction cục bộ: khóa phiếu, kiểm tra version và dòng,
     * xác nhận Location, cộng các bucket tồn, ghi movement rồi chuyển trạng thái RECEIVED.
     */
    @Override
    public SupplierReceiptResponse post(String receiptId, PostSupplierReceiptRequest request, String performedBy) {
        return transactionTemplate.execute(status -> postInsideTransaction(receiptId, request, performedBy));
    }

    private SupplierReceiptResponse postInsideTransaction(String receiptId,
                                                           PostSupplierReceiptRequest request,
                                                           String performedBy) {
        SupplierReceiptHeader header = receiptRepository.findByIdForUpdate(receiptId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND,
                        "Không tìm thấy phiếu nhập: " + receiptId));

        // Retry sau timeout có thể đến khi server đã commit; version + 1 chứng minh là cùng một lần ghi sổ.
        if ("RECEIVED".equals(header.status())) {
            List<SupplierReceiptLineRecord> receivedLines = receiptRepository.findLines(receiptId);
            Map<String, String> requestedLocations = new HashMap<>();
            for (PostSupplierReceiptRequest.Line input : request.lines()) {
                if (requestedLocations.putIfAbsent(input.lineId().trim(), input.locationId().trim()) != null) {
                    throw new BusinessException(ErrorCode.INVALID_OPERATION_CONTENT,
                            "lineId bị lặp trong request: " + input.lineId());
                }
            }
            boolean samePosting = request.expectedVersion() != null
                    && request.expectedVersion() + 1 == header.version()
                    && requestedLocations.size() == receivedLines.size()
                    && receivedLines.stream().allMatch(line ->
                            line.locationId() != null
                                    && line.locationId().equals(requestedLocations.get(line.lineId())));
            if (samePosting) {
                return toResponse(header, receivedLines, receiptRepository.findDocumentAssetIds(receiptId));
            }
            throw new BusinessException(ErrorCode.INVALID_RESOURCE_STATE,
                    "Phiếu nhập đã được ghi sổ trước đó");
        }
        if (!"APPROVED".equals(header.status())) {
            throw new BusinessException(ErrorCode.INVALID_RESOURCE_STATE,
                    "Chỉ được ghi sổ phiếu nhập đã ở trạng thái APPROVED");
        }
        if (request.expectedVersion() == null || header.version() != request.expectedVersion()) {
            throw new BusinessException(ErrorCode.VERSION_CONFLICT,
                    "Phiếu đã thay đổi; hãy tải lại dữ liệu mới nhất trước khi ghi sổ");
        }

        List<SupplierReceiptLineRecord> currentLines = receiptRepository.findLines(receiptId);
        Map<String, SupplierReceiptLineRecord> linesById = currentLines.stream()
                .collect(Collectors.toMap(SupplierReceiptLineRecord::lineId, Function.identity()));
        Map<String, String> locationsByLine = new HashMap<>();
        for (PostSupplierReceiptRequest.Line input : request.lines()) {
            String lineId = input.lineId().trim();
            if (locationsByLine.putIfAbsent(lineId, input.locationId().trim()) != null) {
                throw new BusinessException(ErrorCode.INVALID_OPERATION_CONTENT,
                        "lineId bị lặp trong request: " + lineId);
            }
            if (!linesById.containsKey(lineId)) {
                throw new BusinessException(ErrorCode.INVALID_OPERATION_CONTENT,
                        "lineId không thuộc phiếu nhập này: " + lineId);
            }
        }
        if (!locationsByLine.keySet().equals(linesById.keySet())) {
            throw new BusinessException(ErrorCode.INVALID_OPERATION_CONTENT,
                    "Phải gán đúng một Location cho toàn bộ dòng phiếu nhập");
        }

        List<SupplierReceiptLineRecord> locatedLines = currentLines.stream().map(line -> {
            BigDecimal classified = line.goodQty().add(line.damagedQty()).add(line.inspectionQty());
            if (classified.compareTo(line.actualQty()) != 0) {
                throw new BusinessException(ErrorCode.INVALID_QUANTITY,
                        "Phân loại số lượng không khớp số thực nhận tại dòng " + line.lineId());
            }
            return new SupplierReceiptLineRecord(line.lineId(), line.lineNumber(), line.skuId(), line.uom(),
                    line.expectedQty(), line.lotCode(), line.actualQty(), line.goodQty(), line.damagedQty(),
                    line.inspectionQty(), locationsByLine.get(line.lineId()));
        }).toList();

        // Xác nhận warehouse tồn tại/active và lấy enterpriseId làm khóa phạm vi tồn kho.
        String enterpriseId = warehouseService.getById(header.warehouseId()).enterpriseId();
        List<ClassifiedStockReceipt> stockReceipts = locatedLines.stream()
                .map(line -> new ClassifiedStockReceipt(enterpriseId, header.warehouseId(), line.locationId(),
                        line.skuId(), line.goodQty(), line.damagedQty(), line.inspectionQty(), receiptId, performedBy))
                .toList();

        // Cùng transaction: nếu kiểm tra Location, cập nhật tồn hay ghi movement lỗi thì phiếu vẫn APPROVED.
        receiptRepository.updateLineLocations(receiptId, locatedLines);
        inventoryService.receiveClassifiedStock(stockReceipts);
        if (!receiptRepository.markReceived(receiptId, request.expectedVersion())) {
            throw new BusinessException(ErrorCode.VERSION_CONFLICT,
                    "Phiếu đã thay đổi trạng thái hoặc version trong lúc ghi sổ");
        }

        SupplierReceiptHeader receivedHeader = new SupplierReceiptHeader(header.id(), header.warehouseId(),
                header.supplierRef(), header.operationId(), header.payloadHash(), "RECEIVED",
                header.version() + 1, header.createdAt(), header.approvedBy(), header.approvedAt());
        return toResponse(receivedHeader, locatedLines, receiptRepository.findDocumentAssetIds(receiptId));
    }
}
