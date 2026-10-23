package com.dtthuan3.ecommerce.reviewservice.client;


import com.dtthuan3.ecommerce.reviewservice.exception.BusinessException;
import com.dtthuan3.ecommerce.reviewservice.exception.ServiceUnavailableException;
import com.dtthuan3.ecommerce.reviewservice.exception.UnprocessableEntityException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Gọi File-service (REST) để: (1) kiểm file READY, đúng owner/purpose; (2) tạo binding khi review được duyệt.
 * Tắt mặc định (review.file-verification.enabled=false) vì File-service hiện CHƯA có các endpoint này.
 * Hợp đồng đề xuất:
 * <pre>
 * POST /internal/files/verify   { fileIds, ownerId, purpose } -> { files: [ {fileId, status, ownerId, purpose} ] }
 * POST /internal/files/bindings { ownerType:"REVIEW_REVISION", ownerId, fileIds } -> { bindings: [ {fileId, bindingId} ] }
 * </pre>
 */
@Component
public class FileVerificationClient {

    record VerifyRequest(List<String> fileIds, String ownerId, String purpose) {
    }

    record FileState(String fileId, String status, String ownerId, String purpose) {
    }

    record VerifyResponse(List<FileState> files) {
    }

    record BindRequest(String ownerType, String ownerId, List<String> fileIds) {
    }

    record Binding(String fileId, String bindingId) {
    }

    record BindResponse(List<Binding> bindings) {
    }

    /** Bật khi file-service có /internal/files/verify và /internal/files/bindings. */
    private static final boolean FILE_VERIFICATION_ENABLED = false;
    private static final String FILE_PURPOSE = "REVIEW_IMAGE";

    private final RestClient rest;

    public FileVerificationClient(RestClient.Builder lbBuilder) {
        this.rest = lbBuilder.clone().baseUrl("http://file-service").build();
    }

    public boolean enabled() {
        return FILE_VERIFICATION_ENABLED;
    }

    /** Ném lỗi nếu có file chưa READY / không thuộc người dùng / sai mục đích. No-op khi tắt. */
    public void ensureReady(String ownerId, List<String> fileIds) {
        if (!enabled() || fileIds == null || fileIds.isEmpty()) {
            return;
        }
        VerifyResponse response;
        try {
            response = rest.post().uri("/internal/files/verify")
                    .headers(this::bearer)
                    .body(new VerifyRequest(fileIds, ownerId, FILE_PURPOSE))
                    .retrieve()
                    .body(VerifyResponse.class);
        } catch (RestClientException e) {
            throw new ServiceUnavailableException("FILE_SERVICE_UNAVAILABLE",
                    "Chưa kiểm tra được ảnh, vui lòng thử lại sau");
        }
        Map<String, FileState> byId = new HashMap<>();
        if (response != null && response.files() != null) {
            response.files().forEach(f -> byId.put(f.fileId(), f));
        }
        for (String fileId : fileIds) {
            FileState state = byId.get(fileId);
            if (state == null || !"READY".equalsIgnoreCase(state.status())) {
                throw new UnprocessableEntityException("FILES_NOT_READY", "Ảnh " + fileId + " chưa sẵn sàng");
            }
            if (!ownerId.equals(state.ownerId()) || !FILE_PURPOSE.equals(state.purpose())) {
                throw new UnprocessableEntityException("FILE_NOT_OWNED", "Ảnh " + fileId + " không hợp lệ");
            }
        }
    }

    /** Tạo binding cho các file của revision; trả fileId -> bindingId (rỗng khi tắt). */
    public Map<String, String> bind(String revisionId, List<String> fileIds) {
        Map<String, String> result = new HashMap<>();
        if (!enabled() || fileIds == null || fileIds.isEmpty()) {
            return result;
        }
        try {
            BindResponse response = rest.post().uri("/internal/files/bindings")
                    .headers(this::bearer)
                    .body(new BindRequest("REVIEW_REVISION", revisionId, fileIds))
                    .retrieve()
                    .body(BindResponse.class);
            if (response != null && response.bindings() != null) {
                response.bindings().forEach(b -> result.put(b.fileId(), b.bindingId()));
            }
        } catch (RestClientException e) {
            throw new ServiceUnavailableException("FILE_SERVICE_UNAVAILABLE",
                    "Chưa tạo được liên kết ảnh, vui lòng thử lại sau");
        }
        return result;
    }

    private void bearer(HttpHeaders headers) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwt) {
            headers.setBearerAuth(jwt.getToken().getTokenValue());
        }
    }
}