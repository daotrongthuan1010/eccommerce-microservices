package com.dtthuan3.ecommerce.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception nghiệp vụ chung. Ném ở service layer, GlobalExceptionHandler sẽ map ra ApiResponse.
 *
 * Ví dụ: throw new BusinessException(ErrorCode.NOT_FOUND, "Product not found");
 *        throw new BusinessException("PRODUCT_OUT_OF_STOCK", HttpStatus.CONFLICT, "Hết hàng");
 */
public class BusinessException extends RuntimeException {

    private final String code;
    private final HttpStatus status;

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.code();
        this.status = errorCode.status();
    }

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.code());
        this.code = errorCode.code();
        this.status = errorCode.status();
    }

    public BusinessException(String code, HttpStatus status, String message) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public String getCode() { return code; }
    public HttpStatus getStatus() { return status; }
}
