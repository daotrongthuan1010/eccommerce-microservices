package com.dtthuan3.ecommerce.searchservice.exception;

import com.dtthuan3.ecommerce.common.exception.ErrorCode;
import com.dtthuan3.ecommerce.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.elasticsearch.UncategorizedElasticsearchException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientException;

/**
 * Handler dac thu search-service (§8 moi service co handler rieng, uu tien
 * truoc common GlobalExceptionHandler): loi ES va loi goi product-service
 * tra envelope chuan + traceId MDC, khong lo raw stacktrace ra client.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SearchExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(SearchExceptionHandler.class);

    // Index chua duoc tao / bi xoa (vd ES reset, reindex chua chay): tra 503
    // thay vi 500 de FE tu fallback sang browse cua product-service, khach
    // khong thay "Internal server error" (bug 2026-11-09: products_v2 bi xoa
    // luc 05:48Z, NoSuchIndexException khong phai Uncategorized nen truoc day
    // roi thang xuong GlobalExceptionHandler thanh 500 INTERNAL_ERROR).
    @ExceptionHandler(org.springframework.data.elasticsearch.NoSuchIndexException.class)
    ResponseEntity<ApiResponse<Void>> indexMissing(
            org.springframework.data.elasticsearch.NoSuchIndexException e,
            HttpServletRequest req) {
        log.error("Elasticsearch index missing at {} {}: {}", req.getMethod(), req.getRequestURI(), e.getMessage());
        return body(HttpStatus.SERVICE_UNAVAILABLE, ErrorCode.SERVICE_UNAVAILABLE.code(),
                "Tìm kiếm tạm thời không khả dụng");
    }

    @ExceptionHandler(UncategorizedElasticsearchException.class)
    ResponseEntity<ApiResponse<Void>> esError(UncategorizedElasticsearchException e, HttpServletRequest req) {
        log.error("Elasticsearch error at {} {}: {}", req.getMethod(), req.getRequestURI(), e.getMessage());
        return body(HttpStatus.BAD_GATEWAY, ErrorCode.SERVICE_UNAVAILABLE.code(),
                "Tìm kiếm tạm thời không khả dụng");
    }

    @ExceptionHandler(RestClientException.class)
    ResponseEntity<ApiResponse<Void>> upstreamError(RestClientException e, HttpServletRequest req) {
        log.error("product-service unreachable at {} {}: {}", req.getMethod(), req.getRequestURI(), e.getMessage());
        return body(HttpStatus.BAD_GATEWAY, ErrorCode.SERVICE_UNAVAILABLE.code(),
                "Dịch vụ sản phẩm tạm thời không khả dụng");
    }

    private ResponseEntity<ApiResponse<Void>> body(HttpStatus status, String code, String message) {
        ApiResponse<Void> b = ApiResponse.error(code, message, MDC.get("requestId"));
        return ResponseEntity.status(status).body(b);
    }
}
