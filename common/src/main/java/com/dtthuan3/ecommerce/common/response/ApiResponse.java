package com.dtthuan3.ecommerce.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;

/**
 * Envelope chuẩn enterprise cho mọi service. FE/BFF parse thống nhất:
 * { success, code, message, data, timestamp }
 * data có thể là Page, List hoặc object.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private boolean success;
    private String code;
    private String message;
    private T data;
    private Instant timestamp;
    /** Optional trace id — filled by CorrelationIdFilter / GlobalExceptionHandler if present */
    private String traceId;

    public ApiResponse() { this.timestamp = Instant.now(); }

    public ApiResponse(boolean success, String code, String message, T data) {
        this.success = success;
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = Instant.now();
    }

    // --- factory: success ---
    public static <T> ApiResponse<T> ok(T data) { return new ApiResponse<>(true, "OK", "OK", data); }
    public static <T> ApiResponse<T> ok(String message, T data) { return new ApiResponse<>(true, "OK", message, data); }
    public static ApiResponse<Void> ok() { return new ApiResponse<>(true, "OK", "OK", null); }
    public static <T> ApiResponse<T> created(T data) { return new ApiResponse<>(true, "CREATED", "Created", data); }
    public static <T> ApiResponse<T> accepted(T data) { return new ApiResponse<>(true, "ACCEPTED", "Accepted", data); }

    /** Wrap paged data: { content, totalElements, totalPages, ... } already inside PageResponse */
    public static <T> ApiResponse<PageResponse<T>> paged(PageResponse<T> page) { return ok(page); }
    public static <T> ApiResponse<List<T>> list(List<T> items) { return ok(items); }

    // --- factory: error ---
    public static <T> ApiResponse<T> error(String code, String message) { return new ApiResponse<>(false, code, message, null); }
    public static <T> ApiResponse<T> error(String code, String message, String traceId) {
        ApiResponse<T> r = new ApiResponse<>(false, code, message, null);
        r.setTraceId(traceId);
        return r;
    }

    // --- getters / setters ---
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
}
