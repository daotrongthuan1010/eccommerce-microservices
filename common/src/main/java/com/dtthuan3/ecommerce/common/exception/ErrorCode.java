package com.dtthuan3.ecommerce.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Mã lỗi chuẩn toàn hệ — enterprise.
 * Mỗi service có thể mở rộng bằng BusinessException(code riêng) nhưng nên ưu tiên dùng sẵn
 * để FE/BFF map thống nhất. Tương ứng HTTP status được gắn sẵn để GlobalExceptionHandler tự map.
 */
public enum ErrorCode {
    OK("OK", HttpStatus.OK),
    CREATED("CREATED", HttpStatus.CREATED),
    ACCEPTED("ACCEPTED", HttpStatus.ACCEPTED),
    NO_CONTENT("NO_CONTENT", HttpStatus.NO_CONTENT),

    BAD_REQUEST("BAD_REQUEST", HttpStatus.BAD_REQUEST),
    VALIDATION_ERROR("VALIDATION_ERROR", HttpStatus.BAD_REQUEST),
    INVALID_QUANTITY("INVALID_QUANTITY", HttpStatus.BAD_REQUEST),
    INVALID_TIME_RANGE("INVALID_TIME_RANGE", HttpStatus.BAD_REQUEST),
    INVALID_OPERATION("INVALID_OPERATION", HttpStatus.BAD_REQUEST),
    INVALID_OPERATION_CONTENT("INVALID_OPERATION_CONTENT", HttpStatus.BAD_REQUEST),
    PAYLOAD_TOO_LARGE("PAYLOAD_TOO_LARGE", HttpStatus.PAYLOAD_TOO_LARGE),
    UNPROCESSABLE_ENTITY("UNPROCESSABLE_ENTITY", HttpStatus.UNPROCESSABLE_ENTITY),
    VIRUS_DETECTED("VIRUS_DETECTED", HttpStatus.UNPROCESSABLE_ENTITY),

    UNAUTHORIZED("UNAUTHORIZED", HttpStatus.UNAUTHORIZED),
    FORBIDDEN("FORBIDDEN", HttpStatus.FORBIDDEN),
    RESOURCE_INACTIVE("RESOURCE_INACTIVE", HttpStatus.FORBIDDEN),
    RESOURCE_OWNERSHIP_MISMATCH("RESOURCE_OWNERSHIP_MISMATCH", HttpStatus.FORBIDDEN),

    NOT_FOUND("NOT_FOUND", HttpStatus.NOT_FOUND),
    CONFLICT("CONFLICT", HttpStatus.CONFLICT),
    DUPLICATE_RESOURCE("DUPLICATE_RESOURCE", HttpStatus.CONFLICT),
    INSUFFICIENT_STOCK("INSUFFICIENT_STOCK", HttpStatus.CONFLICT),
    VERSION_CONFLICT("VERSION_CONFLICT", HttpStatus.CONFLICT),
    INVALID_RESOURCE_STATE("INVALID_RESOURCE_STATE", HttpStatus.CONFLICT),
    RATE_LIMITED("RATE_LIMITED", HttpStatus.TOO_MANY_REQUESTS),

    STORAGE_ERROR("STORAGE_ERROR", HttpStatus.BAD_GATEWAY),
    DEPENDENCY_ERROR("DEPENDENCY_ERROR", HttpStatus.BAD_GATEWAY),
    SERVICE_UNAVAILABLE("SERVICE_UNAVAILABLE", HttpStatus.SERVICE_UNAVAILABLE),
    INTERNAL_ERROR("INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String code;
    private final HttpStatus status;

    ErrorCode(String code, HttpStatus status) {
        this.code = code;
        this.status = status;
    }

    public String code() { return code; }
    public HttpStatus status() { return status; }

    public static ErrorCode fromCode(String code) {
        for (ErrorCode e : values()) if (e.code.equalsIgnoreCase(code)) return e;
        return INTERNAL_ERROR;
    }
}
