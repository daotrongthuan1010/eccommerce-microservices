package com.dtthuan3.ecommerce.fileservice.exception;

import com.dtthuan3.ecommerce.common.exception.ErrorCode;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * Handler riêng của file-service (giữ lại các lỗi đặc thù: virus, storage, validation file).
 * Lỗi chung (VALIDATION_ERROR, FORBIDDEN, UNAUTHORIZED, INTERNAL_ERROR) đã có
 * common.GlobalExceptionHandler — nhưng để handler này ưu tiên cho file-service
 * thì vẫn xử lý để trả message đặc thù (ví dụ 422 virus, 502 storage).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(FileValidationException.class)
    ResponseEntity<Map<String, Object>> validation(FileValidationException e) {
        return body(HttpStatus.BAD_REQUEST, ErrorCode.BAD_REQUEST.code(), e.getMessage());
    }

    @ExceptionHandler({MissingServletRequestPartException.class, MissingServletRequestParameterException.class})
    ResponseEntity<Map<String, Object>> missing(Exception e) {
        return body(HttpStatus.BAD_REQUEST, ErrorCode.BAD_REQUEST.code(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String, Object>> methodArgNotValid(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b).orElse(e.getMessage());
        return body(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR.code(), msg);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<Map<String, Object>> constraintViolation(ConstraintViolationException e) {
        return body(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR.code(), e.getMessage());
    }

    @ExceptionHandler(FileNotFoundInStorageException.class)
    ResponseEntity<Map<String, Object>> notFound(FileNotFoundInStorageException e) {
        return body(HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND.code(), e.getMessage());
    }

    @ExceptionHandler(VirusDetectedException.class)
    ResponseEntity<Map<String, Object>> virusDetected(VirusDetectedException e) {
        log.warn("Virus detected: {}", e.getMessage());
        return body(HttpStatus.UNPROCESSABLE_ENTITY, "VIRUS_DETECTED", e.getMessage());
    }

    @ExceptionHandler(VirusScanException.class)
    ResponseEntity<Map<String, Object>> virusScan(VirusScanException e) {
        log.error("Virus scan error", e);
        return body(HttpStatus.SERVICE_UNAVAILABLE, ErrorCode.SERVICE_UNAVAILABLE.code(), "Virus scan temporarily unavailable");
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<Map<String, Object>> accessDenied(AccessDeniedException e) {
        return body(HttpStatus.FORBIDDEN, ErrorCode.FORBIDDEN.code(), "Access denied");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<Map<String, Object>> tooLarge(MaxUploadSizeExceededException e) {
        return body(HttpStatus.PAYLOAD_TOO_LARGE, ErrorCode.BAD_REQUEST.code(), "File exceeds the maximum allowed size");
    }

    @ExceptionHandler(StorageException.class)
    ResponseEntity<Map<String, Object>> storage(StorageException e) {
        log.error("Storage error", e);
        return body(HttpStatus.BAD_GATEWAY, ErrorCode.SERVICE_UNAVAILABLE.code(), "Storage backend error");
    }

    private ResponseEntity<Map<String, Object>> body(HttpStatus status, String code, String message) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("timestamp", Instant.now().toString());
        m.put("status", status.value());
        m.put("code", code);
        m.put("error", status.getReasonPhrase());
        m.put("message", message);
        return ResponseEntity.status(status).body(m);
    }
}
