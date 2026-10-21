package com.dtthuan3.ecommerce.inventoryservice.integration;

import com.dtthuan3.ecommerce.common.exception.BusinessException;
import com.dtthuan3.ecommerce.common.exception.ErrorCode;
import com.dtthuan3.ecommerce.inventoryservice.port.ReceiptDocumentValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Collection;

/** Adapter gọi File Service; service nghiệp vụ chỉ phụ thuộc interface ReceiptDocumentValidator. */
@Component
@RequiredArgsConstructor
public class FileServiceReceiptDocumentValidator implements ReceiptDocumentValidator {
    /** Builder dùng chung của Spring để cấu hình HTTP client theo dependency injection. */
    private final RestClient.Builder restClientBuilder;

    /** URL có thể đổi theo môi trường; local mặc định trỏ tới File Service trên máy chủ dev. */
    @Value("${inventory.integration.file-service-base-url:http://162.4.177.91:8095}")
    private String fileServiceBaseUrl;

    /**
     * Kiểm tra lần lượt metadata của từng object trước khi khóa receipt để tránh giữ transaction khi gọi mạng.
     * File Service hiện trả metadata object, chưa có trường lifecycle READY; object tồn tại được xem là đã upload xong.
     */
    @Override
    public void requireUploaded(Collection<String> assetIds, String bearerToken) {
        if (assetIds == null || assetIds.isEmpty()) return;
        if (bearerToken == null || bearerToken.isBlank()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED,
                    "Không lấy được access token để xác minh chứng từ trên File Service");
        }

        RestClient client = restClientBuilder.clone().baseUrl(fileServiceBaseUrl).build();
        for (String assetId : assetIds) {
            try {
                client.get()
                        .uri(uriBuilder -> uriBuilder.path("/files/info").queryParam("key", assetId).build())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                        .retrieve()
                        .toBodilessEntity();
            } catch (HttpClientErrorException.NotFound ex) {
                throw new BusinessException(ErrorCode.INVALID_OPERATION_CONTENT,
                        "Chứng từ chưa được tải lên hoặc không tồn tại trên File Service: " + assetId);
            } catch (RestClientException ex) {
                throw new BusinessException(ErrorCode.DEPENDENCY_ERROR,
                        "Không thể xác minh chứng từ trên File Service; hãy thử lại sau");
            }
        }
    }
}
