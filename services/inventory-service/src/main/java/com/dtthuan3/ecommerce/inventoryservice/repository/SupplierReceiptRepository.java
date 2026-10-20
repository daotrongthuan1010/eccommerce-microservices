package com.dtthuan3.ecommerce.inventoryservice.repository;

import com.dtthuan3.ecommerce.inventoryservice.repository.model.SupplierReceiptHeader;
import com.dtthuan3.ecommerce.inventoryservice.repository.model.SupplierReceiptLineRecord;
import com.dtthuan3.ecommerce.inventoryservice.dto.response.SupplierReceiptSummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Cổng truy cập dữ liệu phiếu nhập. Interface mô tả thao tác nghiệp vụ cần dùng,
 * không để service phụ thuộc vào Spring Data JPA hoặc cách lưu trữ cụ thể.
 */
public interface SupplierReceiptRepository {
    /** Tìm header theo operationId để trả lại kết quả cũ khi caller retry. */
    Optional<SupplierReceiptHeader> findByOperationId(String operationId);

    /** Đọc header theo ID để dựng trang chi tiết phiếu nhập. */
    Optional<SupplierReceiptHeader> findById(String receiptId);

    /** Liệt kê tóm tắt phiếu thuộc một kho, không tải các dòng chi tiết của từng phiếu. */
    Page<SupplierReceiptSummaryResponse> findByWarehouseId(String warehouseId, Pageable pageable);

    /** Đọc và khóa header trong transaction để serialize cập nhật số lượng trên cùng một phiếu. */
    Optional<SupplierReceiptHeader> findByIdForUpdate(String receiptId);

    /** Chèn header nếu operationId chưa tồn tại; trả false nếu một request khác đã claim khóa trước. */
    boolean insertHeaderIfAbsent(SupplierReceiptHeader header);

    /** Chèn toàn bộ dòng phiếu bằng batch trong transaction hiện tại. */
    void insertLines(String receiptId, List<SupplierReceiptLineRecord> lines);

    /** Chèn các liên kết tài liệu bằng batch trong transaction hiện tại. */
    void insertDocuments(String receiptId, List<String> assetIds);

    /** Đọc dòng hàng theo line_number để thứ tự nghiệp vụ được giữ nguyên. */
    List<SupplierReceiptLineRecord> findLines(String receiptId);

    /** Đọc danh sách ID tài liệu theo thứ tự ổn định để retry trả response nhất quán. */
    List<String> findDocumentAssetIds(String receiptId);

    /** Tăng version có điều kiện để chỉ một request với expectedVersion hiện tại được thắng. */
    boolean incrementVersion(String receiptId, long expectedVersion);

    /** Chuyển APPROVED sang RECEIVED và tăng version theo điều kiện để chống ghi sổ hai lần. */
    boolean markReceived(String receiptId, long expectedVersion);

    /** Chuyển SUBMITTED sang APPROVED, ghi người/thời điểm duyệt và tăng version nguyên tử. */
    boolean markApproved(String receiptId, long expectedVersion, String approvedBy, Instant approvedAt);

    /** Chuyển DRAFT sang SUBMITTED bằng compare-and-set sau khi service đã hoàn tất mọi kiểm tra. */
    boolean markSubmitted(String receiptId, long expectedVersion);

    /** Cập nhật số lượng thực nhận và phân loại cho các dòng trong phiếu. */
    void updateActuals(String receiptId, List<SupplierReceiptLineRecord> lines);

    /** Ghi Location nhận hàng đã được kiểm tra cho toàn bộ dòng trước khi cộng tồn. */
    void updateLineLocations(String receiptId, List<SupplierReceiptLineRecord> lines);

    /** Nối thêm chứng từ bằng insert chống trùng theo unique key receipt_id + asset_id. */
    void appendDocuments(String receiptId, List<String> assetIds);
}
