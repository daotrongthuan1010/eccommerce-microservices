package com.dtthuan3.ecommerce.common.exception;

import com.dtthuan3.ecommerce.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

/**
 * Handler chuẩn enterprise — auto-scan cho mọi service MVC (Servlet).
 * Không tác động tới gateway (WebFlux). Thứ tự LOW để handler cục bộ của service
 * có thể override nếu cần.
 */
@RestControllerAdvice
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@Order(Ordered.LOWEST_PRECEDENCE)
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException ex, HttpServletRequest req) {
        if (ex.getStatus().is5xxServerError()) log.error("[{}] {} at {} {}", ex.getCode(), ex.getMessage(), req.getMethod(), req.getRequestURI(), ex);
        else log.warn("[{}] {} at {} {}", ex.getCode(), ex.getMessage(), req.getMethod(), req.getRequestURI());
        return build(ex.getStatus(), ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(ResourceNotFoundException ex, HttpServletRequest req) {
        log.warn("NOT_FOUND at {} {}: {}", req.getMethod(), req.getRequestURI(), ex.getMessage());
        return build(HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND.code(), ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream().map(this::formatFieldError).collect(Collectors.joining("; "));
        if (msg.isBlank()) msg = "Validation failed";
        log.warn("VALIDATION_ERROR: {}", msg);
        return build(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR.code(), msg);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraint(ConstraintViolationException ex) {
        log.warn("VALIDATION_ERROR: {}", ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR.code(), ex.getMessage());
    }

    @ExceptionHandler({MissingServletRequestParameterException.class, MissingServletRequestPartException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(Exception ex) {
        log.warn("BAD_REQUEST: {}", ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, ErrorCode.BAD_REQUEST.code(), ex.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotReadable(HttpMessageNotReadableException ex) {
        log.warn("BAD_REQUEST not readable: {}", ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, ErrorCode.BAD_REQUEST.code(), "Malformed request body");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUpload(MaxUploadSizeExceededException ex) {
        log.warn("PAYLOAD_TOO_LARGE: {}", ex.getMessage());
        return build(HttpStatus.PAYLOAD_TOO_LARGE, ErrorCode.PAYLOAD_TOO_LARGE.code(), "File exceeds the maximum allowed size");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleForbidden(AccessDeniedException ex) {
        log.warn("FORBIDDEN: {}", ex.getMessage());
        return build(HttpStatus.FORBIDDEN, ErrorCode.FORBIDDEN.code(), "Forbidden");
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnauthorized(AuthenticationException ex) {
        log.warn("UNAUTHORIZED: {}", ex.getMessage());
        return build(HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHORIZED.code(), "Unauthorized");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception ex, HttpServletRequest req) {
        log.error("INTERNAL_ERROR at {} {}: {}", req.getMethod(), req.getRequestURI(), ex.getMessage(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_ERROR.code(), "Internal server error");
    }

    private ResponseEntity<ApiResponse<Void>> build(HttpStatus status, String code, String message) {
        ApiResponse<Void> body = ApiResponse.error(code, message, MDC.get("requestId"));
        return ResponseEntity.status(status).body(body);
    }

    private String formatFieldError(FieldError fe) { return fe.getField() + ": " + fe.getDefaultMessage(); }
}
