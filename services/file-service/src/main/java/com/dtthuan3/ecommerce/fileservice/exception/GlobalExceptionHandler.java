package com.dtthuan3.ecommerce.fileservice.exception;

import com.dtthuan3.ecommerce.common.exception.ErrorCode;
import com.dtthuan3.ecommerce.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

/**
 * Handler đặc thù file-service — ưu tiên trước common GlobalExceptionHandler
 * (Ordered HIGHEST). Trả ApiResponse chuẩn + traceId từ MDC.
 * Lỗi chung không có ở đây sẽ rơi về common handler.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(FileValidationException.class)
    ResponseEntity<ApiResponse<Void>> validation(FileValidationException e, HttpServletRequest req) {
        log.warn("BAD_REQUEST at {} {}: {}", req.getMethod(), req.getRequestURI(), e.getMessage());
        return body(HttpStatus.BAD_REQUEST, ErrorCode.BAD_REQUEST.code(), e.getMessage());
    }

    @ExceptionHandler({MissingServletRequestPartException.class, MissingServletRequestParameterException.class})
    ResponseEntity<ApiResponse<Void>> missing(Exception e) {
        return body(HttpStatus.BAD_REQUEST, ErrorCode.BAD_REQUEST.code(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiResponse<Void>> methodArgNotValid(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b).orElse(e.getMessage());
        return body(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR.code(), msg);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ApiResponse<Void>> constraintViolation(ConstraintViolationException e) {
        return body(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR.code(), e.getMessage());
    }

    @ExceptionHandler(FileNotFoundInStorageException.class)
    ResponseEntity<ApiResponse<Void>> notFound(FileNotFoundInStorageException e, HttpServletRequest req) {
        log.warn("NOT_FOUND at {} {}: {}", req.getMethod(), req.getRequestURI(), e.getMessage());
        return body(HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND.code(), e.getMessage());
    }

    @ExceptionHandler(VirusDetectedException.class)
    ResponseEntity<ApiResponse<Void>> virusDetected(VirusDetectedException e) {
        log.warn("VIRUS_DETECTED: {}", e.getMessage());
        return body(HttpStatus.UNPROCESSABLE_ENTITY, ErrorCode.VIRUS_DETECTED.code(), e.getMessage());
    }

    @ExceptionHandler(VirusScanException.class)
    ResponseEntity<ApiResponse<Void>> virusScan(VirusScanException e) {
        log.error("Virus scan error", e);
        return body(HttpStatus.SERVICE_UNAVAILABLE, ErrorCode.SERVICE_UNAVAILABLE.code(), "Virus scan temporarily unavailable");
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiResponse<Void>> accessDenied(AccessDeniedException e) {
        return body(HttpStatus.FORBIDDEN, ErrorCode.FORBIDDEN.code(), "Access denied");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<ApiResponse<Void>> tooLarge(MaxUploadSizeExceededException e) {
        return body(HttpStatus.PAYLOAD_TOO_LARGE, ErrorCode.PAYLOAD_TOO_LARGE.code(), "File exceeds the maximum allowed size");
    }

    @ExceptionHandler(StorageException.class)
    ResponseEntity<ApiResponse<Void>> storage(StorageException e) {
        log.error("STORAGE_ERROR", e);
        return body(HttpStatus.BAD_GATEWAY, ErrorCode.STORAGE_ERROR.code(), "Storage backend error");
    }

    private ResponseEntity<ApiResponse<Void>> body(HttpStatus status, String code, String message) {
        ApiResponse<Void> b = ApiResponse.error(code, message, MDC.get("requestId"));
        return ResponseEntity.status(status).body(b);
    }
}
